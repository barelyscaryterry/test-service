variable "aws_region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "us-east-1"
}

variable "service_name" {
  description = "Name of this service. Drives resource naming/tagging. Future backend services copying this pattern override this."
  type        = string
  default     = "test-service"
}

variable "environment" {
  description = "Deployment environment (e.g. test, prod)."
  type        = string
  default     = "test"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC created for this service."
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets, one per AZ used."
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "dynamodb_table_name" {
  description = "DynamoDB table name. Must match test-service.ddb.table in application.properties."
  type        = string
  default     = "beers"
}

variable "dynamodb_deletion_protection" {
  description = "Whether to enable deletion protection on the DynamoDB table."
  type        = bool
  default     = true
}

variable "ec2_instance_type" {
  description = "EC2 instance type for the app host."
  type        = string
  default     = "t3.micro"
}

variable "app_port" {
  description = "Port the application listens on."
  type        = number
  default     = 8080
}

variable "app_ingress_cidrs" {
  description = "CIDR blocks allowed to reach the app port. Defaults to VPC-internal only; widen later (e.g. to a future capture-proxy CIDR/SG) as needed."
  type        = list(string)
  default     = ["10.0.0.0/16"]
}

variable "root_volume_size_gb" {
  description = "Root EBS volume size (GiB) for the EC2 instance. Must be >= the AL2023 AMI's snapshot size (30GB as of writing)."
  type        = number
  default     = 30
}

variable "cloudwatch_namespace" {
  description = "CloudWatch metrics namespace. Must match management.cloudwatch.metrics.export.namespace in application.properties."
  type        = string
  default     = "test-service"
}

variable "tags" {
  description = "Extra tags merged into every resource's default tags."
  type        = map(string)
  default     = {}
}
