data "aws_iam_policy_document" "ec2_trust" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ec2" {
  name               = "${local.name_prefix}-ec2-role"
  assume_role_policy = data.aws_iam_policy_document.ec2_trust.json
}

# Scoped to exactly the operations BeerRepository uses (put/get/scan) on
# exactly the beers table - patch is a read-modify-write via get()+put(),
# so no UpdateItem is needed.
data "aws_iam_policy_document" "dynamodb_access" {
  statement {
    actions = [
      "dynamodb:GetItem",
      "dynamodb:PutItem",
      "dynamodb:Scan",
    ]
    resources = [aws_dynamodb_table.beers.arn]
  }
}

resource "aws_iam_policy" "dynamodb_access" {
  name   = "${local.name_prefix}-dynamodb-access"
  policy = data.aws_iam_policy_document.dynamodb_access.json
}

resource "aws_iam_role_policy_attachment" "dynamodb_access" {
  role       = aws_iam_role.ec2.name
  policy_arn = aws_iam_policy.dynamodb_access.arn
}

# Enables SSM Session Manager access (no SSH keypair, no inbound port 22).
resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# The CodeDeploy agent running on-instance pulls deployment bundles
# directly from S3 - this is all it needs beyond what SSM already grants.
data "aws_iam_policy_document" "codedeploy_agent_access" {
  statement {
    actions   = ["s3:GetObject", "s3:GetObjectVersion"]
    resources = ["${aws_s3_bucket.deploy_artifacts.arn}/*"]
  }
  statement {
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.deploy_artifacts.arn]
  }
}

resource "aws_iam_policy" "codedeploy_agent_access" {
  name   = "${local.name_prefix}-codedeploy-agent-access"
  policy = data.aws_iam_policy_document.codedeploy_agent_access.json
}

resource "aws_iam_role_policy_attachment" "codedeploy_agent_access" {
  role       = aws_iam_role.ec2.name
  policy_arn = aws_iam_policy.codedeploy_agent_access.arn
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${local.name_prefix}-ec2-profile"
  role = aws_iam_role.ec2.name
}
