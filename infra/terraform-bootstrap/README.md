# test-service Terraform bootstrap

Creates the prerequisites the *main* `infra/terraform/` config needs before
it can run in GitHub Actions:

- An S3 bucket + DynamoDB lock table for remote Terraform state.
- A GitHub OIDC identity provider + IAM role that GitHub Actions assumes
  (no long-lived AWS keys stored in GitHub).

This has to be applied **once, manually, by a human** — it can't itself run
in the CI pipeline it's setting up (chicken-and-egg), and it uses local
state deliberately, separate from the main config's remote state.

## Usage

```bash
cd infra/terraform-bootstrap
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

Note the outputs (`state_bucket_name`, `lock_table_name`,
`github_actions_role_arn`) — the main config's backend block and the
GitHub Actions workflow both need them.

## Note on credentials

This was applied using AWS root credentials in this account, which AWS
recommends against for routine work — consider creating a dedicated IAM
user/role for infra work going forward, now that this bootstrap role
exists as the actual thing CI uses day to day.
