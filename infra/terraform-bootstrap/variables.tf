variable "aws_region" {
  description = "AWS region for the state bucket/lock table and OIDC role."
  type        = string
  default     = "us-east-1"
}

variable "service_name" {
  description = "Name of the service this bootstrap infra supports."
  type        = string
  default     = "test-service"
}

variable "github_repo" {
  description = "GitHub repo in \"owner/name\" form allowed to assume the deploy role."
  type        = string
  default     = "barelyscaryterry/test-service"
}

variable "state_bucket_name" {
  description = "Globally-unique S3 bucket name for Terraform state."
  type        = string
  default     = "test-service-tfstate-966676241497"
}
