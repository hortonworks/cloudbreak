---
name: cdp-docs
description: Official CDP Public Cloud documentation lookup. Use this skill when any agent (Developer, Architect, Reviewer) needs to verify official Cloudera requirements, release notes, or API specifications for the Public Cloud.
---

# CDP Public Cloud Documentation (Reference)

This skill provides a structured map to the official Cloudera CDP Public Cloud documentation. Use it to inform architectural decisions, verify network requirements, or check the latest release notes.

## 🔗 Primary Documentation Hubs
- **Main Index**: [CDP Public Cloud Home](https://docs.cloudera.com/cdp-public-cloud/cloud/index.html)
- **Management Console**: [Management Console Home](https://docs.cloudera.com/management-console/cloud/index.html) (Environments, Users, Credentials)
- **SDX**: [Shared Data Experience](https://docs.cloudera.com/sdx/cloud/index.html) (Datalakes, Security, Governance)
- **Data Hub**: [Data Hub Home](https://docs.cloudera.com/data-hub/cloud/index.html) (Cluster Templates, Workloads)
- **Release Notes**: [CDP Release Notes](https://docs.cloudera.com/cdp-public-cloud/cloud/release-notes/topics/cdp-release-notes.html)

## Search strategy
When asked to verify a requirement (e.g., "What are the Azure VNet requirements for CDP?"):
1. **Identify the hub**: Determine if the topic belongs to Management Console, SDX, or a specific Data Service.
2. **Retrieve the page**: Open or fetch the relevant hub URL (browser, `curl`, or whatever HTTP/fetch capability your environment provides).
3. **Synthesis**: Ground your implementation plan in the retrieved documentation.

## 📂 Logical Path Mapping
- **Environments**: `management-console/cloud/environments/`
- **Network**: `management-console/cloud/network/`
- **Azure Specifics**: `management-console/cloud/azure-configuration/`
- **AWS Specifics**: `management-console/cloud/aws-configuration/`
- **GCP Specifics**: `management-console/cloud/gcp-configuration/`
- **Security/IAM**: `management-console/cloud/user-management/`
