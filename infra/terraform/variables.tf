variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "project" {
  type    = string
  default = "aisly"
}

variable "db_username" {
  type      = string
  sensitive = true
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "auth_issuer_uri" {
  type = string
}

variable "auth_audience" {
  type    = string
  default = "aisly-api"
}

