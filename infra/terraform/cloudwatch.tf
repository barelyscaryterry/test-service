# Dashboard for the app metrics published by Micrometer (see
# management.cloudwatch.metrics.export.* in application.properties) plus the
# DynamoDB/EC2 metrics AWS already emits for free. SEARCH expressions are used
# instead of hardcoded dimensions because Micrometer's CloudWatch registry tags
# each series (uri/method/outcome/status, resilience4j name/kind) and those
# dimension values aren't known statically here.
resource "aws_cloudwatch_dashboard" "this" {
  dashboard_name = local.name_prefix

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "text"
        x      = 0
        y      = 0
        width  = 24
        height = 1
        properties = {
          markdown = "# ${local.name_prefix}"
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 1
        width  = 12
        height = 6
        properties = {
          title   = "HTTP request rate"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          metrics = [
            [{ expression = "SEARCH('{${var.cloudwatch_namespace},method,outcome,status,uri} MetricName=\"http.server.requests.count\"', 'Sum', 60)", label = "" }]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 1
        width  = 12
        height = 6
        properties = {
          title   = "HTTP request latency (p99)"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          metrics = [
            [{ expression = "SEARCH('{${var.cloudwatch_namespace},method,outcome,status,uri} MetricName=\"http.server.requests.max\"', 'p99', 60)", label = "" }]
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 7
        width  = 8
        height = 6
        properties = {
          title   = "Circuit breaker calls by kind (beerApi)"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = true
          metrics = [
            [{ expression = "SEARCH('{${var.cloudwatch_namespace},kind,name} MetricName=\"resilience4j.circuitbreaker.calls.count\" name=\"beerApi\"', 'Sum', 60)", label = "" }]
          ]
        }
      },
      {
        type   = "metric"
        x      = 8
        y      = 7
        width  = 8
        height = 6
        properties = {
          title   = "Retry calls by kind (beerApi)"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = true
          metrics = [
            [{ expression = "SEARCH('{${var.cloudwatch_namespace},kind,name} MetricName=\"resilience4j.retry.calls.count\" name=\"beerApi\"', 'Sum', 60)", label = "" }]
          ]
        }
      },
      {
        type   = "metric"
        x      = 16
        y      = 7
        width  = 8
        height = 6
        properties = {
          title   = "Rate limiter calls by kind (beerApi)"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = true
          metrics = [
            [{ expression = "SEARCH('{${var.cloudwatch_namespace},kind,name} MetricName=\"resilience4j.ratelimiter.calls.count\" name=\"beerApi\"', 'Sum', 60)", label = "" }]
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 13
        width  = 12
        height = 6
        properties = {
          title   = "DynamoDB - beers table"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          metrics = [
            ["AWS/DynamoDB", "ConsumedReadCapacityUnits", "TableName", aws_dynamodb_table.beers.name, { stat = "Sum" }],
            ["AWS/DynamoDB", "ConsumedWriteCapacityUnits", "TableName", aws_dynamodb_table.beers.name, { stat = "Sum" }],
            ["AWS/DynamoDB", "ThrottledRequests", "TableName", aws_dynamodb_table.beers.name, { stat = "Sum" }],
            ["AWS/DynamoDB", "SystemErrors", "TableName", aws_dynamodb_table.beers.name, { stat = "Sum" }],
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 13
        width  = 12
        height = 6
        properties = {
          title   = "EC2 - app host"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          metrics = [
            ["AWS/EC2", "CPUUtilization", "InstanceId", aws_instance.app.id, { stat = "Average" }],
            ["AWS/EC2", "StatusCheckFailed", "InstanceId", aws_instance.app.id, { stat = "Maximum" }],
          ]
        }
      },
    ]
  })
}
