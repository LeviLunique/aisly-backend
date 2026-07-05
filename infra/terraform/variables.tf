variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "project" {
  type    = string
  default = "aisly"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "billing_alert_email" {
  type    = string
  default = "levi.lunique@outlook.com"
}

variable "billing_alarm_threshold_usd" {
  type    = number
  default = 1
}

variable "db_username" {
  type    = string
  default = "aisly"
}

variable "auth_issuer_uri" {
  type    = string
  default = ""
}

variable "auth_jwk_set_uri" {
  type    = string
  default = "http://auth:8080/.well-known/jwks.json"
}

variable "auth_audience" {
  type    = string
  default = "aisly-api"
}

variable "application_jar_path" {
  type    = string
  default = "../../build/libs/aisly-backend-0.1.0-1-SNAPSHOT.jar"
}

variable "app_version_label" {
  type    = string
  default = "0.1.0-1-SNAPSHOT"
}

variable "authserver_jar_path" {
  type    = string
  default = "../../../backend-dev/build/libs/authserver-0.0.1-SNAPSHOT.jar"
}

variable "authserver_version_label" {
  type    = string
  default = "0.0.1-SNAPSHOT"
}

variable "app_instance_type" {
  type    = string
  default = "t4g.micro"
}
