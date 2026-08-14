# test-service infra (Terraform)

Self-contained AWS infra for `test-service`: VPC, public subnets, IGW, a
DynamoDB Gateway Endpoint, the `beers` DynamoDB table, and an EC2 instance
(with an IAM role scoped to that table) to eventually run the app on.

This is intentionally separate from any `capture-proxy` infra — no shared
VPC, no shared state — so each backend service can own its infra
independently as more get added.

## Usage

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars   # adjust if needed
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

State is local (`terraform.tfstate`, gitignored) — no remote backend is
configured yet. Migrating to S3 later is a drop-in change.

## Connecting to the instance

No SSH keypair and no inbound port 22 — access is via AWS Systems Manager
Session Manager instead, authorized through IAM rather than a key file:

```bash
aws ssm start-session --target "$(terraform output -raw ec2_instance_id)"
```

Requires AWS CLI credentials with `ssm:StartSession` permission on the
instance. The SSM Agent is preinstalled on the Amazon Linux 2023 AMI used
here; no extra setup on the instance is required.

## Known follow-ups (not implemented in this pass)

- **Jar deploy mechanism**: how the built jar actually gets onto the
  instance and runs (user_data bootstrap, SSM Run Command, or CodeDeploy)
  is deliberately not decided yet — see the `TODO(deploy-mechanism)` in
  `ec2.tf`.
- **capture-proxy connectivity**: once `capture-proxy` has its own
  Terraform-managed VPC/security group, widen `app_ingress_cidrs` in
  `security_groups.tf` (or switch to an SG reference / VPC peering) to
  allow it in specifically — see the `TODO(capture-proxy-integration)`
  there.
