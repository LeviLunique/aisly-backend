data "aws_caller_identity" "current" {}

resource "aws_ecr_repository" "api" {
  name                 = "${var.project}-backend"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_sqs_queue" "account_deleted" {
  name                      = "${var.project}-account-deleted"
  message_retention_seconds = 1209600
}

resource "aws_sns_topic" "account_events" {
  name = "${var.project}-account-events"
}

resource "aws_sns_topic_subscription" "account_deleted_queue" {
  topic_arn = aws_sns_topic.account_events.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.account_deleted.arn
}

resource "aws_db_instance" "postgres" {
  identifier             = "${var.project}-postgres"
  engine                 = "postgres"
  engine_version         = "18"
  instance_class         = "db.t4g.micro"
  allocated_storage      = 20
  db_name                = "aisly"
  username               = var.db_username
  password               = var.db_password
  skip_final_snapshot    = false
  publicly_accessible    = false
  storage_encrypted      = true
  backup_retention_period = 7
}

resource "aws_elastic_beanstalk_application" "api" {
  name        = "${var.project}-backend"
  description = "Aisly modular backend API"
}

output "ecr_repository_url" {
  value = aws_ecr_repository.api.repository_url
}

output "account_events_topic_arn" {
  value = aws_sns_topic.account_events.arn
}

output "account_deleted_queue_url" {
  value = aws_sqs_queue.account_deleted.url
}

