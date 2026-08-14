resource "aws_security_group" "ec2" {
  name        = "${local.name_prefix}-ec2-sg"
  description = "Security group for the ${var.service_name} app host"
  vpc_id      = aws_vpc.this.id

  # No port 22 rule - access is via SSM Session Manager only.

  dynamic "ingress" {
    for_each = var.app_ingress_cidrs
    content {
      description = "App traffic on ${var.app_port}"
      from_port   = var.app_port
      to_port     = var.app_port
      protocol    = "tcp"
      cidr_blocks = [ingress.value]
    }
  }

  # TODO(capture-proxy-integration): once capture-proxy's VPC/SG exist as
  # Terraform resources, widen app_ingress_cidrs (or switch to a security
  # group reference / VPC peering) to allow it in specifically, rather than
  # broadening to the public internet.

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.name_prefix}-ec2-sg"
  }
}
