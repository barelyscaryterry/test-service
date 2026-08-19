# test-service infra (Terraform)

Self-contained AWS infra for `test-service`: VPC, public subnets, IGW, a
DynamoDB Gateway Endpoint, the `beers` DynamoDB table, an EC2 instance
running the app, a CodeDeploy application/deployment group that ships new
jars to it, and a CloudWatch dashboard for the app's metrics.

This is intentionally separate from any `capture-proxy` infra — no shared
VPC, no shared state — so each backend service can own its infra
independently as more get added.

## How changes get applied

State is remote (S3 + DynamoDB lock table, provisioned once by
`infra/terraform-bootstrap` — see that directory's README/comments). Applies
run in CI, not by hand:

- **Pull requests** targeting `main` that touch `infra/terraform/**` get a
  `terraform plan` posted as a PR comment (`.github/workflows/terraform.yml`).
- **Merges to `main`** run `terraform apply` automatically, gated behind the
  `production` GitHub Environment (requires reviewer approval — configure
  reviewers in repo settings).

CI assumes `test-service-github-actions-terraform` via GitHub OIDC — no
long-lived AWS credentials are stored in the repo. That role's permissions
live in `infra/terraform-bootstrap/oidc.tf`; if you add a resource type this
config doesn't already manage (a new AWS service, a new set of actions), the
apply will fail with `AccessDenied` until that role is granted the matching
permissions there.

To run `plan`/`apply` locally (e.g. to preview a change before opening a
PR), you need credentials with equivalent permissions — usually easiest via
`aws sso login` / `aws login` with an admin or power-user role in this
account:

```bash
cd infra/terraform
terraform init
terraform plan
```

Don't run `terraform apply` locally against shared state unless you have a
specific reason to bypass the PR/CI flow — it skips the plan-review step and
can race a concurrent CI apply.

## How a code change gets deployed

Separately from Terraform: `.github/workflows/deploy.yml` builds the jar,
zips it with `appspec.yml` and `scripts/`, uploads it to the
`deploy-artifacts` S3 bucket, and triggers a CodeDeploy in-place deployment
to the EC2 instance. It runs whenever `Java CI with Maven` succeeds on
`main` (not gated on the Terraform workflow) — see the `hooks` in
`appspec.yml` for what runs on the instance during a deploy
(`stop_service.sh` / `start_service.sh` / `validate_service.sh`).

Because the deploy pipeline isn't sequenced after the infra pipeline, a
deploy that lands while Terraform is mid-replacement of the EC2 instance
(e.g. a `user_data` change) can transiently fail with `NO_INSTANCES` — retry
it once the instance is back up if that happens.

## Connecting to the instance

No SSH keypair and no inbound port 22 — access is via AWS Systems Manager
Session Manager instead, authorized through IAM rather than a key file:

```bash
aws ssm start-session --target "$(terraform output -raw ec2_instance_id)"
```

Requires AWS CLI credentials with `ssm:StartSession` permission on the
instance. The SSM Agent is preinstalled on the Amazon Linux 2023 AMI used
here; no extra setup on the instance is required.

## Observability

The app publishes metrics to CloudWatch under the `test-service` namespace
(via Micrometer — see `management.cloudwatch.metrics.export.*` in
`application.properties`), and `cloudwatch.tf` provisions a dashboard
(`terraform output cloudwatch_dashboard_url`) covering HTTP request
rate/latency, Resilience4j circuit breaker/retry/rate-limiter activity,
DynamoDB, and EC2 host metrics.

## Known follow-ups (not implemented in this pass)

- **capture-proxy connectivity**: once `capture-proxy` has its own
  Terraform-managed VPC/security group, widen `app_ingress_cidrs` in
  `security_groups.tf` (or switch to an SG reference / VPC peering) to
  allow it in specifically — see the `TODO(capture-proxy-integration)`
  there.
- **Deploy/infra pipeline race**: `deploy.yml` isn't sequenced after
  `terraform.yml` completing, so a deploy can race an in-flight instance
  replacement (see above). Chaining `deploy.yml` off the `Terraform`
  workflow instead of (or in addition to) `Java CI with Maven` would close
  this gap.
