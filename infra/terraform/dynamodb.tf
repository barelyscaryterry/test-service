# Partition key "id" (String), no sort key, no GSIs - matches
# BeerRepository.java's ATTR_ID usage and its GetItem/PutItem/Scan calls.
resource "aws_dynamodb_table" "beers" {
  name         = var.dynamodb_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  deletion_protection_enabled = var.dynamodb_deletion_protection

  tags = {
    Name = "${local.name_prefix}-beers"
  }
}
