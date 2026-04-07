---
name: cb-cloud-providers
description: Operate read-first against AWS, Azure, and optionally GCP from the terminal or cloud MCP tools—instances by region, related compute/network resources, and troubleshooting. For Cloudbreak work in cloud-aws-*, cloud-azure, cloud-gcp modules.
---

# Cloudbreak Cloud Providers (AWS / Azure / GCP)

Use this skill when you need **live cloud state** to triage tickets, compare real resources to code paths, or answer “what is running where?” **Default stance: read-only** APIs and CLI; no creates, deletes, or stops unless the user explicitly asks.

## Install CLIs

- **AWS**: [AWS CLI v2](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
- **Azure**: [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)
- **GCP** (optional): [Google Cloud SDK](https://cloud.google.com/sdk/docs/install)

## Connect and authenticate

You connect by giving the CLI (or an MCP server that wraps the same SDKs) a **valid identity**. Never put access keys or client secrets into the repo, Jira, or agent prompts—use OS env vars, `az login`, or your org’s SSO flow.

### AWS

1. **Access keys + named profile** (common for local dev when your org allows it):  
   `aws configure --profile <profile-name>`  
   Stores defaults under **`~/.aws/credentials`** and **`~/.aws/config`**. Then either export **`export AWS_PROFILE=<profile-name>`** or pass **`--profile <profile-name>`** on each command.

2. **IAM Identity Center (SSO)** (common in enterprises):  
   Configure once, e.g. **`aws configure sso`** (your admin provides SSO start URL, region, and permission set). Then before working:  
   **`aws sso login --profile <profile-name>`**  
   Use that profile via **`AWS_PROFILE`** or **`--profile`**.

3. **Environment variables** (CI or automation):  
   **`AWS_ACCESS_KEY_ID`**, **`AWS_SECRET_ACCESS_KEY`**, optional **`AWS_SESSION_TOKEN`**, and **`AWS_DEFAULT_REGION`** (or pass **`--region`** every time).

4. **Verify you are who you think you are**:  
   **`aws sts get-caller-identity`**

If a command says “Unable to locate credentials,” fix auth first (profile, SSO login, or env vars)—not IAM policy, until identity resolves.

### Azure

1. **Interactive (browser)** — usual for laptops:  
   **`az login`**  
   If you have several tenants: **`az login --tenant <tenant-id>`**

2. **Pick the subscription** (many accounts have more than one):  
   **`az account list --output table`**  
   **`az account set --subscription <subscription-id-or-name>`**  
   **`az account show`** — confirms current sub + tenant.

3. **Service principal** (automation / headless): either  
   **`az login --service-principal -u <app-id> -p <client-secret> --tenant <tenant-id>`**  
   or set env vars your tooling expects (often **`AZURE_CLIENT_ID`**, **`AZURE_CLIENT_SECRET`**, **`AZURE_TENANT_ID`**) and use **`az login --service-principal`** per your team’s doc—names can vary slightly by SDK.

4. **Verify**: **`az account show`** (and **`az ad signed-in-user show`** if you need the signed-in principal).

### GCP (optional)

**`gcloud auth login`** (browser), then **`gcloud config set project <project-id>`**. Verify: **`gcloud auth list`** and **`gcloud config get-value project`**.

### MCP servers

If you use an **AWS or Azure MCP** in Cursor/Gemini, it typically picks up the **same** credentials: default AWS credential chain / **`AWS_PROFILE`**, or Azure **`az login`** session (or SP env vars). Check that server’s README for required env vars; behavior should match “CLI works on this machine.”

## Working scope

- Prefer a **known region / subscription / resource group** from the ticket, runbook, or user; avoid huge account-wide scans unless necessary.

## Repo context

Cloudbreak implementation code maps roughly to **`cloud-aws-*`**, **`cloud-azure/`**, **`cloud-gcp/`**, and **`cloud-api/`** (see root `GEMINI.md`). Use this skill for **account reality**; use **`cb-architect`** / module **`GEMINI.md`** for **code-level** behavior.

---

## Amazon Web Services (AWS)

### List / inspect EC2 instances in a region

```bash
aws ec2 describe-instances --region <region> --query 'Reservations[].Instances[].[InstanceId,State.Name,InstanceType,Tags[?Key==`Name`].Value|[0],PrivateIpAddress,PublicIpAddress]' --output table
```

Filter by running only:

```bash
aws ec2 describe-instances --region <region> --filters "Name=instance-state-name,Values=running" --output table
```

Filter by tag (example: `Name` contains prefix):

```bash
aws ec2 describe-instances --region <region> --filters "Name=tag:Name,Values=*<prefix>*" --output table
```

### Other common read-only checks

| Use case | Example direction |
|----------|-------------------|
| ASG / capacity | `aws autoscaling describe-auto-scaling-groups --region <region>` |
| Load balancers | `aws elbv2 describe-load-balancers --region <region>` |
| RDS instances | `aws rds describe-db-instances --region <region>` |
| EKS (if relevant) | `aws eks list-clusters --region <region>` |
| STS who-am-I | `aws sts get-caller-identity` |

Summarize **account ID, region, and resource IDs**; do not paste full JSON into Jira without need.

---

## Microsoft Azure

### List VMs in a subscription / region

```bash
az account show --output table
az vm list --subscription <sub-id-or-name> --show-details --output table
```

Filter by resource group:

```bash
az vm list --resource-group <rg> --subscription <sub-id-or-name> --show-details --output table
```

List resource groups (find scope):

```bash
az group list --subscription <sub-id-or-name> --output table
```

### Other common read-only checks

| Use case | Example direction |
|----------|-------------------|
| VM sizes & state | `az vm list-skus --location <region> --size <family>` (quotas); `az vm get-instance-view -g <rg> -n <name>` |
| Network | `az network nic list -g <rg>`, `az network public-ip list -g <rg>` |
| Managed disks | `az disk list -g <rg>` |

Use **`--subscription`** consistently when the user has multiple tenants.

---

## Google Cloud Platform (GCP) — optional

Relevant to **`cloud-gcp/`**. Same read-first rules.

```bash
gcloud config get-value project
gcloud compute instances list --zones=<zone-or-filter>
```

Prefer **`gcloud`** **list** / **describe**; avoid **`delete`** without explicit user approval.

---

## Safety and compliance

- **Read-only default** — Describe, list, get; no **terminate**, **stop**, **delete**, **revoke**, or **put-bucket-policy** unless the user clearly requests a mutating operation.
- **Production** — Call out if commands may touch prod subscriptions/accounts; suggest change windows or runbooks from Confluence when applicable.
- **Data handling** — Treat instance metadata, IPs, and ARNs as sensitive in external channels; redact in public PRs.

## Pairing

- **`cb-triage` / `cb-junior-dev`** — Ground “what Cloudbreak should do” with “what the cloud currently shows.”
- **`cdp-docs`** — Customer-facing network and CDP requirements vs your account layout.
