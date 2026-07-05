data "aws_caller_identity" "current" {}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnet" "default_a" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }

  filter {
    name   = "availability-zone"
    values = ["${var.aws_region}a"]
  }
}

data "aws_ami" "amazon_linux_2023_arm64" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-arm64"]
  }

  filter {
    name   = "architecture"
    values = ["arm64"]
  }

  filter {
    name   = "state"
    values = ["available"]
  }
}

resource "random_password" "db_password" {
  length           = 32
  special          = true
  override_special = "_-"
}

resource "random_password" "internal_webhook_secret" {
  length           = 48
  special          = true
  override_special = "_-"
}

resource "random_password" "jwt_hmac_secret" {
  length  = 64
  special = false
}

resource "aws_sns_topic" "billing_alerts" {
  name = "${var.project}-${var.environment}-billing-alerts"
}

resource "aws_sns_topic_subscription" "billing_email" {
  topic_arn = aws_sns_topic.billing_alerts.arn
  protocol  = "email"
  endpoint  = var.billing_alert_email
}

resource "aws_cloudwatch_metric_alarm" "estimated_charges" {
  alarm_name          = "${var.project}-${var.environment}-estimated-charges"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "EstimatedCharges"
  namespace           = "AWS/Billing"
  period              = 21600
  statistic           = "Maximum"
  threshold           = var.billing_alarm_threshold_usd
  alarm_description   = "Estimated AWS charges exceeded ${var.billing_alarm_threshold_usd} USD."
  alarm_actions       = [aws_sns_topic.billing_alerts.arn]
  treat_missing_data  = "notBreaching"

  dimensions = {
    Currency = "USD"
  }
}

resource "aws_security_group" "app" {
  name        = "${var.project}-${var.environment}-app"
  description = "Aisly ${var.environment} single-node application"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "Aisly API"
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = var.allowed_ingress_cidr_blocks
  }

  ingress {
    description = "Aisly AuthServer"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = var.allowed_ingress_cidr_blocks
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_iam_role" "app_ec2" {
  name = "${var.project}-${var.environment}-app-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "app_ssm" {
  role       = aws_iam_role.app_ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy" "app_s3_artifacts" {
  name = "${var.project}-${var.environment}-app-artifacts"
  role = aws_iam_role.app_ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject"
        ]
        Resource = "${aws_s3_bucket.deploy.arn}/artifacts/*"
      }
    ]
  })
}

resource "aws_iam_instance_profile" "app" {
  name = "${var.project}-${var.environment}-app-ec2"
  role = aws_iam_role.app_ec2.name
}

resource "aws_s3_bucket" "deploy" {
  bucket = "${var.project}-${var.environment}-deploy-${data.aws_caller_identity.current.account_id}-${var.aws_region}"
}




resource "aws_s3_bucket_versioning" "deploy" {
  bucket = aws_s3_bucket.deploy.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_object" "application_jar" {
  bucket      = aws_s3_bucket.deploy.id
  key         = "artifacts/${var.app_version_label}/aisly-backend.jar"
  source      = var.application_jar_path
  source_hash = filemd5(var.application_jar_path)
}

resource "aws_s3_object" "authserver_jar" {
  bucket      = aws_s3_bucket.deploy.id
  key         = "artifacts/authserver/${var.authserver_version_label}/authserver.jar"
  source      = var.authserver_jar_path
  source_hash = filemd5(var.authserver_jar_path)
}

resource "aws_instance" "app" {
  ami                         = data.aws_ami.amazon_linux_2023_arm64.id
  instance_type               = var.app_instance_type
  subnet_id                   = data.aws_subnet.default_a.id
  vpc_security_group_ids      = [aws_security_group.app.id]
  iam_instance_profile        = aws_iam_instance_profile.app.name
  associate_public_ip_address = true
  user_data_replace_on_change = false

  metadata_options {
    http_tokens   = "required" # IMDSv2 only — user-data already uses tokens
    http_endpoint = "enabled"
  }

  root_block_device {
    volume_size           = 16
    volume_type           = "gp3"
    encrypted             = true
    delete_on_termination = true
  }

  user_data = templatefile("${path.module}/user-data.sh.tftpl", {
    aws_region              = var.aws_region
    artifact_bucket         = aws_s3_bucket.deploy.id
    artifact_key            = aws_s3_object.application_jar.key
    auth_artifact_key       = aws_s3_object.authserver_jar.key
    db_name                 = "aisly"
    db_username             = var.db_username
    db_password             = random_password.db_password.result
    internal_webhook_secret = random_password.internal_webhook_secret.result
    jwt_hmac_secret         = random_password.jwt_hmac_secret.result
  })

  tags = {
    Name = "${var.project}-${var.environment}-app"
  }
}





output "app_base_url" {
  value = "http://${aws_instance.app.public_dns}:8081"
}

output "auth_base_url" {
  value = "http://${aws_instance.app.public_dns}:8080"
}

output "app_public_ip" {
  value = aws_instance.app.public_ip
}

output "billing_topic_arn" {
  value = aws_sns_topic.billing_alerts.arn
}
