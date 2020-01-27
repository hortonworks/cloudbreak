# Cloudbreak Monitoring in Terraform

Cloudbreak related monitoring is maintained in Terraform scripts (HCL). Monitors are grouped in dashboards, and all dashboards are added to a dashboard list. 

## Input Parameters

The following environment variables need to be set for Terraform to be able to communicate with Datadog.

```
export TF_VAR_datadog_api_key=<datadog api key>
export TF_VAR_datadog_app_key=<datadog app key>
```

## Deploy Configurations

Initialize the Terraform module.

```
terraform init
```

Generate Terraform plan file and review the plan.

```
terraform plan -out monitoring.tfplan
```

Apply the plan.

```
terraform apply "monitoring.tfplan"
```