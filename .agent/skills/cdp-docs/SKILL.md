---
name: cdp-docs
description: Official CDP Public Cloud documentation lookup. Use this skill when any agent (Developer, Architect, Reviewer) needs to verify official Cloudera requirements, release notes, or API specifications for the Public Cloud.
---

# CDP Public Cloud Documentation (Reference)

This skill provides a structured map to the official Cloudera CDP Public Cloud documentation. Use it to inform architectural decisions, verify network requirements, or check the latest release notes.

All URLs below have been verified live. The docs site navigation is JavaScript-driven so WebFetch only retrieves body content — use the explicit URLs here rather than trying to crawl the nav tree.

**Base domain**: `https://docs.cloudera.com/`

---

## Release Notes

> There is **no single aggregated CDP release notes page** — each service has its own what's-new index. The `cdp-public-cloud/cloud/release-notes/` and `sdx/cloud/` paths return 404.

### Management Console (covers Environments, Datalake, FreeIPA)
- **Index**: `management-console/cloud/release-notes/topics/mc-whats-new.html` ✅
- **Individual releases**: `management-console/cloud/release-notes/topics/mc_whats_new_NNN.html`
  - 2025: 102 (Jan 15) → 103 → 104 → 105 → 106 → 106.1 → 107 → 108 → 109 → 110 → 111 → 112 → 113 (Dec 4)
  - 2026: 114 (Jan 9) → 115 → 116 → 117 (Mar 31, latest as of Apr 2026)
- **Datalake-specific entries live here** — there is no separate Datalake/SDX release notes page.

### Data Hub
- **Index**: `data-hub/cloud/release-notes/topics/dh-whats-new.html` ✅
- **Individual releases**: `data-hub/cloud/release-notes/topics/dh_whats_new_NN.html`
  - 2025: 36 (Jan 30) → 37 → 38 → 39 → 40 → 41 → 42 → 42a → 43 (Nov 5)
  - 2026: 44 (Jan 9) → 45 (Mar 31, latest)

---

## Environments

- **Overview** (what an environment is, component diagram): `management-console/cloud/environments/topics/mc-environments.html` ✅
- **AWS account requirements** (permissions, services, outbound network): `cdp-public-cloud/cloud/requirements-aws/topics/mc-requirements-aws.html` ✅
  - AWS permissions detail: `../topics/mc-aws-permissions.html`
  - AWS services list: `../topics/mc-aws-req-services.html`
  - AWS outbound access destinations: `../topics/mc-outbound_access_requirements.html`
  - Workload UI access (DNS *.cloudera.site): `../topics/mc-access-to-cdp-workload-endpoints.html`
- **Azure account requirements**: `cdp-public-cloud/cloud/requirements-azure/topics/mc-azure-credential.html` ✅
  - Azure resources and services: `../topics/mc-azure-resources-and-services.html`
  - Migrating Azure Load Balancers: `management-console/cloud/environments-azure/topics/mc-migrating-azure-load-balancers.html`
- **GCP account requirements**: `cdp-public-cloud/cloud/requirements-gcp/topics/mc-requirements-gcp.html` ✅
  - GCP permissions: `../topics/mc-gcp-permissions.html`
  - GCP services: `../topics/mc-gcp-req-services.html`

---

## Credentials

Credential docs are split by cloud provider under `management-console/cloud/credentials-{provider}/`:

- **Azure — create app-based credential**: `management-console/cloud/credentials-azure/topics/mc-create-app-based-credential.html` ✅
- **Azure — modify credential**: `management-console/cloud/credentials-azure/topics/mc-credential-modify-azure.html` ✅
- **AWS credential prereqs** live under the requirements path above (`mc-aws-permissions.html`).
- **GCP — service account prereqs** (create SA, assign roles, generate JSON key): `cdp-public-cloud/cloud/requirements-gcp/topics/mc-gcp-service-account-provisioning-credential.html` ✅
- **GCP — register credential in Management Console UI**: `management-console/cloud/credentials-gcp/topics/mc-create-provisioning-credential-gcp.html` ✅
  - Requires `EnvironmentCreator` role; key type must be JSON (not P12).

---

## FreeIPA

FreeIPA does **not** have its own top-level doc section (`management-console/cloud/freeipa/` returns 404). FreeIPA content appears in:
- Environment setup pages (architecture selection, LDAP security settings)
- Management Console release notes (search `mc_whats_new_NNN.html` for "FreeIPA")
- The environments overview page (`mc-environments.html`) describes FreeIPA as an auto-provisioned environment component.

---

## Datalake

- **Overview** (components: Ranger, Atlas, Knox, Hive Metastore, cloud storage): `management-console/cloud/data-lakes/topics/mc-data-lake.html` ✅
- **Storage configuration**: `management-console/cloud/data-lakes/topics/mc-data-lake-storage.html` ✅
- **Scaling**: `management-console/cloud/data-lakes/topics/mc-data-lake-scale.html` ✅
- **Repair**: `management-console/cloud/data-lakes/topics/mc-data-lake-repair.html` ✅
- **Security overview**: `cdp-public-cloud/cloud/security-overview/topics/security-data-lake-security.html` ✅

> The `sdx/cloud/` path (e.g. `sdx/cloud/index.html`) returns **404**. All Datalake documentation lives under `management-console/cloud/data-lakes/`.

### Ranger RAZ (Fine-Grained Cloud Storage Authorization)

RAZ enables Apache Ranger policies over cloud storage instead of coarse IDBroker mappings. **Must be enabled at environment registration — cannot be added to an existing environment.**

**AWS (S3)** — index: `management-console/cloud/fine-grained-access-control-aws/index.html` ✅
- Intro & what RAZ enables: `topics/raz-aws-intro.html`
- Requirements (Runtime 7.2.11+ required; use `DATALAKE_ADMIN_ROLE` or `RAZ_ROLE`; do NOT set IDBroker mappings for workload users): `topics/raz-aws-requirements.html`
- Register via UI (toggle "Ranger authorization for S3" in Data Access and Audit section): `topics/raz-aws-register-cdp-webinterface.html` ✅
- Register via CLI: `topics/raz-aws-register-cdp-cli.html`
- Create Ranger policies (S3 policy via `cm_s3` service + Hive URL auth policy): `topics/raz-aws-create-ranger-policy.html` ✅
- Troubleshooting (AccessDeniedException, Hive/Spark failures, token expiration): `topics/raz-aws-troubleshooting.html`
- Manage existing RAZ environment: `topics/mc-managing_a_raz_enabled_aws_environment.html`

**Azure (ADLS Gen2)** — index: `management-console/cloud/fine-grained-access-control-azure/index.html` ✅
- Intro: `topics/raz-adls-intro.html`
- Create RAZ managed identity: `topics/raz-adls-managed-identity.html`
- Create custom Azure role for RAZ: `topics/raz-adls-policydef-customrole.html`
- Register via UI: `topics/raz-adls-register-cdp-webinterface.html`
- Register via CLI: `topics/raz-adls-register-cdp-cli.html`
- Ranger policies for ADLS: `topics/raz-adls-ranger-policies.html`
- Troubleshooting: `topics/raz-adls-troubleshooting.html`
- Manage existing RAZ environment: `topics/mc-managing_a_raz_enabled_azure_environment.html`

---

## Data Hub

- **Release notes index**: `data-hub/cloud/release-notes/topics/dh-whats-new.html` ✅ (see release notes section above)

---

## Network Setup

Network docs are spread across provider-specific requirement paths and a private-subnet section:

- **AWS outbound access destinations**: `cdp-public-cloud/cloud/requirements-aws/topics/mc-outbound_access_requirements.html` ✅ (relative from AWS requirements base)
- **Azure load balancers for Datalake and Data Hub**: `management-console/cloud/connection-to-private-subnets/topics/mc-azure_load_balancers_in_data_lakes_and_data_hubs.html` ✅
- **Azure network reference architecture (diagrams)**: `cdp-public-cloud/cloud/azure-refarch/topics/cdp-pc-azure-refarch-architecture-diagrams.html` ✅
- **Workload UI / DNS access requirements**: `cdp-public-cloud/cloud/requirements-aws/topics/mc-access-to-cdp-workload-endpoints.html` ✅

> `management-console/cloud/network/` returns **404**. Use the provider-specific paths above.

---

## Getting Started Guides

There is no single getting-started page (`management-console/cloud/getting-started/` returns 404). The practical on-ramp is:
1. Start with the requirements page for your cloud provider (links above).
2. Then follow to the credential creation page for that provider.
3. Then the environments overview.

Provider-specific quickstart guidance is embedded within the requirements and credential pages rather than in a dedicated guide.

---

## Search Strategy

### Step 1 — Search first, don't guess paths

The docs site has a search at `https://docs.cloudera.com/search/`. Always start here rather than constructing URLs by hand — guessed paths return 404 silently.

**Search URL pattern:**
```
https://docs.cloudera.com/search/?q=QUERY&f-form_factor=Cloudera%20on%20cloud&enableQuerySyntax=true
```

- `q=` — your search terms (URL-encoded, spaces as `%20`)
- `f-form_factor=Cloudera%20on%20cloud` — **use this filter for all Cloudbreak / CDP Public Cloud questions**; it scopes results to Public Cloud docs and cuts out on-prem noise

**Example** (Ranger RAZ):
```
https://docs.cloudera.com/search/?q=ranger%20raz&f-form_factor=Cloudera%20on%20cloud&enableQuerySyntax=true
```

> **Important**: The search page is JavaScript-rendered. WebFetch returns the empty shell — it cannot see the results. Use one of the two fallbacks below.

### Step 2 — Agent-accessible search fallbacks

**Option A — WebSearch (preferred):**
Use WebSearch with a `site:docs.cloudera.com` query to replicate the search:
```
site:docs.cloudera.com ranger raz "Cloudera on cloud"
```
This returns real indexed URLs that can then be fetched with WebFetch.

**Option B — Anchor + follow links:**
Start from a verified URL in this skill for the relevant area, fetch it, and follow `Related information` links in the body. Each working page reveals adjacent topic URLs.

### Step 3 — For release notes specifically

Use the index page to get the version number, then fetch the individual numbered page:
- MC (covers Datalake/Environments/FreeIPA): `mc-whats-new.html` → `mc_whats_new_NNN.html`
- Data Hub: `dh-whats-new.html` → `dh_whats_new_NN.html`
