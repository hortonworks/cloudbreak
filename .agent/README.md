# MCP setup for Cloudbreak agents

This folder holds Gemini-oriented skills and workflows. Many of those workflows assume **Model Context Protocol (MCP)** servers for **GitHub**, **Atlassian (Jira & Confluence)**, and optionally **AWS** and **Azure** so agents can read issues, PRs, checks, and live cloud state (CLI-shaped calls via the official AWS proxy or Azure’s MCP server).

This document explains how to **configure those MCP servers safely** and **where** to put the configuration.

**Recommendation:** Run MCP servers with **Docker** where an image exists (GitHub, AWS proxy, Azure MCP, Atlassian). You get a pinned runtime, no **Node.js / `npx`** on the host, and the same pattern across machines. Install [Docker Desktop](https://docs.docker.com/get-docker/) (or your org’s supported engine) and ensure you can pull the registries referenced below (`ghcr.io`, `mcr.microsoft.com`, `public.ecr.aws`, etc.).

## Security first

- **Never commit real tokens** to git. Use **environment variables** or a **local-only** config file that is in `.gitignore`.
- **Never paste PATs or API tokens** into issues, PR descriptions, or chat logs you do not control.
- If a token may have been exposed, **revoke it immediately** in GitHub / Atlassian and create a new one.
- Prefer **fine-scoped** tokens with only the permissions you need.

## GitHub MCP (GitHub Enterprise Server only)

Cloudbreak uses **GitHub Enterprise Server** at **`https://github.infra.cloudera.com`**. The GitHub MCP server must call the **REST API** at **`https://github.infra.cloudera.com/api/v3`** (see `--gh-host` in the sample below).

Official server image: `ghcr.io/github/github-mcp-server` (often run via Docker).

### Create a Personal Access Token on GitHub Enterprise Server

1. In a browser, open **`https://github.infra.cloudera.com`** and sign in.
2. Open your profile (**avatar**, top right) → **Settings**.
3. In the left sidebar, open **Developer settings** → **Personal access tokens**.
4. Create a token using the type your organization allows (**Fine-grained** or **Classic**—follow internal policy).
5. Grant only the scopes your agent needs (for example: repository contents, pull requests, and checks for read-only triage; add issue/write scopes only if required).
6. If your org uses **SAML SSO**, open the token’s details and click **Configure SSO** / **Authorize** for the right organization so API calls succeed.
7. **Copy the token once** and store it in a password manager or shell env; you cannot view it again later.

The MCP server does not use `https://github.com`; all API traffic goes to **`https://github.infra.cloudera.com/api/v3`** via the configured host.

### Pass the token without hard-coding it

**Option A — environment variables (recommended)**

Set before starting Cursor (or in your shell profile):

```bash
export GITHUB_PERSONAL_ACCESS_TOKEN="ghp_REPLACE_ME"
# Optional: some setups read this name instead; align with your MCP server docs
export GH_TOKEN="$GITHUB_PERSONAL_ACCESS_TOKEN"
```

Reference the variable in MCP config (see sample below). **Do not** put the literal token value in a tracked file.

**Option B — local `mcp.json` only on your machine**

Keep MCP config outside the repo, or use a gitignored path such as `.cursor/mcp.local.json` if your client supports including it.

### Sample GitHub MCP entry (placeholders only)

Use this **`--gh-host`** value so the MCP server talks to Enterprise, not github.com:

```json
"github": {
  "command": "docker",
  "args": [
    "run",
    "-i",
    "--rm",
    "-e",
    "GITHUB_PERSONAL_ACCESS_TOKEN",
    "ghcr.io/github/github-mcp-server",
    "stdio",
    "--gh-host",
    "https://github.infra.cloudera.com/api/v3"
  ],
  "env": {
    "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_PERSONAL_ACCESS_TOKEN}"
  }
}
```

If your MCP client does not expand `${VAR}` in JSON, either:

- omit `"env"` and rely on the host process inheriting `GITHUB_PERSONAL_ACCESS_TOKEN`, or  
- use your client’s documented variable substitution.

Docker must be installed and able to pull from `ghcr.io`.

## AWS MCP (`mcp-proxy-for-aws`)

The [**AWS MCP proxy**](https://public.ecr.aws/mcp-proxy-for-aws/mcp-proxy-for-aws) runs in Docker, connects to AWS’s hosted MCP endpoint, and uses the **same credential chain** as the AWS CLI (profiles and SSO under **`~/.aws`**). Align **`AWS_PROFILE`**, **`--profile`**, and the **`AWS_REGION` metadata** with how you work locally.

- **Never commit** access keys or session tokens; keep them in **`~/.aws`** (or your org’s auth flow) only.
- The **`-v ...:/app/.aws:ro`** mount must be an **absolute path** to your AWS directory (Docker does not reliably expand `~`).
- **`--metadata AWS_REGION=...`** sets default Region metadata for the proxy; override per command in the agent when you need another region.

### Sample AWS MCP entry (placeholders)

Use your real home directory in the volume mount and a profile you have configured (SSO, access keys, etc.):

```json
"aws-mcp": {
  "command": "docker",
  "args": [
    "run",
    "-i",
    "--rm",
    "-v",
    "/Users/YOUR_USER/.aws:/app/.aws:ro",
    "-e",
    "AWS_PROFILE=YOUR_AWS_PROFILE",
    "public.ecr.aws/mcp-proxy-for-aws/mcp-proxy-for-aws:latest",
    "https://aws-mcp.us-east-1.api.aws/mcp",
    "--profile",
    "YOUR_AWS_PROFILE",
    "--metadata",
    "AWS_REGION=us-west-2"
  ],
  "env": {
    "AWS_PROFILE": "YOUR_AWS_PROFILE"
  }
}
```

Docker must be installed and able to pull from `public.ecr.aws`. If **`aws sso login`** applies to your profile, run it on the host before starting the MCP server.

## Azure MCP (Docker — `mcr.microsoft.com/azure-sdk/azure-mcp`)

Microsoft publishes the [**Azure MCP Server**](https://learn.microsoft.com/en-us/azure/developer/azure-mcp-server/overview) as a container image: [`mcr.microsoft.com/azure-sdk/azure-mcp`](https://mcr.microsoft.com/artifact/mar/azure-sdk/azure-mcp). The Docker path is the one [documented upstream](https://github.com/microsoft/mcp/blob/main/servers/Azure.Mcp.Server/README.md#docker) for MCP clients.

The container authenticates with Azure using [**EnvironmentCredential**](https://learn.microsoft.com/dotnet/api/azure.identity.environmentcredential) variables (typically a **service principal**), not by reusing **`az login`** files from the host. Treat any `.env` you pass to Docker like a secret—**do not commit** it.

### 1. Create a local `.env` for the container

Use values your Azure admin provides (service principal with least privilege for the tools you need):

```bash
AZURE_TENANT_ID={YOUR_AZURE_TENANT_ID}
AZURE_CLIENT_ID={YOUR_AZURE_CLIENT_ID}
AZURE_CLIENT_SECRET={YOUR_AZURE_CLIENT_SECRET}
```

Optional subscription scoping and other variables are described in Microsoft’s Azure MCP and **EnvironmentCredential** docs; adjust to your tenant’s policy.

### 2. Sample Azure MCP entry (Docker)

Replace `/full/path/to/azure-mcp.env` with an **absolute** path to your `.env` file (Docker does not reliably expand `~`).

```json
"azure-mcp": {
  "command": "docker",
  "args": [
    "run",
    "-i",
    "--rm",
    "--env-file",
    "/full/path/to/azure-mcp.env",
    "mcr.microsoft.com/azure-sdk/azure-mcp:latest"
  ]
}
```

Your MCP client may show this server under a different internal id (e.g. with a `user-` prefix). Rename the key if your client requires a specific pattern.

For **Entra ID**, sovereign clouds, or auth edge cases, see Microsoft’s [**Troubleshooting** guide](https://github.com/microsoft/mcp/blob/main/servers/Azure.Mcp.Server/TROUBLESHOOTING.md) (including the Docker / Entra section).

### 3. Azure CLI for humans (optional)

For **read-only checks** without MCP, **`az login`** and **`az account set`** are still the right workflow—same as **`cb-cloud-providers`**. That is separate from the Docker MCP server’s **EnvironmentCredential** model.

## Atlassian MCP — Jira and Confluence (Docker — `ghcr.io/sooperset/mcp-atlassian`)

Use one MCP server for **both** Jira and Confluence Cloud. The upstream project is [**mcp-atlassian**](https://github.com/sooperset/mcp-atlassian); the maintainers publish **`ghcr.io/sooperset/mcp-atlassian:latest`** ([installation / Docker](https://mcp-atlassian.soomiles.com/docs/installation)). Docker also lists a catalog build as **`mcp/atlassian`**—align image name with what your security team approves.

Your org may standardize a different image or vendor wrapper; substitute the image reference and env vars per IT docs.

### Create an Atlassian API token (Cloud)

1. Sign in at [Atlassian account security](https://id.atlassian.com/manage-profile/security/api-tokens).
2. **Create API token**, label it (e.g. `cursor-mcp-atlassian`), copy it once.
3. Use your **Atlassian account email** and site URLs, for example:
   - Jira: `https://cloudera.atlassian.net`
   - Confluence: `https://cloudera.atlassian.net/wiki`

### Required environment variables (Cloud — typical)

| Variable | Purpose |
|----------|---------|
| `JIRA_URL` | Jira Cloud site URL (no `/wiki`) |
| `JIRA_USERNAME` | Atlassian account email |
| `JIRA_API_TOKEN` | API token |
| `CONFLUENCE_URL` | Confluence Cloud URL (usually `https://<site>.atlassian.net/wiki`) |
| `CONFLUENCE_USERNAME` | Same email as Jira (typical) |
| `CONFLUENCE_API_TOKEN` | Same or separate token per your policy |

Set them in the MCP client’s `env` block (as below) or use a **gitignored** `--env-file` with Docker.

### Sample Atlassian MCP entry (Docker)

Passes secrets from the client `env` into the container (same pattern as [upstream docs](https://mcp-atlassian.soomiles.com/docs/installation)):

```json
"atlassian-mcp": {
  "command": "docker",
  "args": [
    "run",
    "-i",
    "--rm",
    "-e", "JIRA_URL",
    "-e", "JIRA_USERNAME",
    "-e", "JIRA_API_TOKEN",
    "-e", "CONFLUENCE_URL",
    "-e", "CONFLUENCE_USERNAME",
    "-e", "CONFLUENCE_API_TOKEN",
    "ghcr.io/sooperset/mcp-atlassian:latest"
  ],
  "env": {
    "JIRA_URL": "https://cloudera.atlassian.net",
    "JIRA_USERNAME": "you@cloudera.com",
    "JIRA_API_TOKEN": "${JIRA_API_TOKEN}",
    "CONFLUENCE_URL": "https://cloudera.atlassian.net/wiki",
    "CONFLUENCE_USERNAME": "you@cloudera.com",
    "CONFLUENCE_API_TOKEN": "${CONFLUENCE_API_TOKEN}"
  }
}
```

**Server/Data Center** and OAuth flows use different variables—see [**Authentication**](https://mcp-atlassian.soomiles.com/docs/authentication) in the upstream docs.

## Combined example shape (no real secrets)

Your MCP config file might look like this structurally:

```json
{
  "mcpServers": {
    "github": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "-e",
        "GITHUB_PERSONAL_ACCESS_TOKEN",
        "ghcr.io/github/github-mcp-server",
        "stdio",
        "--gh-host",
        "https://github.infra.cloudera.com/api/v3"
      ],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_PERSONAL_ACCESS_TOKEN}"
      }
    },
    "atlassian-mcp": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "-e", "JIRA_URL",
        "-e", "JIRA_USERNAME",
        "-e", "JIRA_API_TOKEN",
        "-e", "CONFLUENCE_URL",
        "-e", "CONFLUENCE_USERNAME",
        "-e", "CONFLUENCE_API_TOKEN",
        "ghcr.io/sooperset/mcp-atlassian:latest"
      ],
      "env": {
        "JIRA_URL": "https://cloudera.atlassian.net",
        "JIRA_USERNAME": "${JIRA_USERNAME}",
        "JIRA_API_TOKEN": "${JIRA_API_TOKEN}",
        "CONFLUENCE_URL": "https://cloudera.atlassian.net/wiki",
        "CONFLUENCE_USERNAME": "${JIRA_USERNAME}",
        "CONFLUENCE_API_TOKEN": "${JIRA_API_TOKEN}"
      }
    },
    "aws-mcp": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "-v",
        "/Users/YOUR_USER/.aws:/app/.aws:ro",
        "-e",
        "AWS_PROFILE=YOUR_AWS_PROFILE",
        "public.ecr.aws/mcp-proxy-for-aws/mcp-proxy-for-aws:latest",
        "https://aws-mcp.us-east-1.api.aws/mcp",
        "--profile",
        "YOUR_AWS_PROFILE",
        "--metadata",
        "AWS_REGION=us-west-2"
      ],
      "env": {
        "AWS_PROFILE": "YOUR_AWS_PROFILE"
      }
    },
    "azure-mcp": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "--env-file",
        "/full/path/to/azure-mcp.env",
        "mcr.microsoft.com/azure-sdk/azure-mcp:latest"
      ]
    }
  }
}
```

Rename server keys (`github` / `atlassian-mcp` / `aws-mcp` / `azure-mcp`) if your skills or client expect different names (e.g. `user-github`, `user-aws-mcp`, `user-atlassian-mcp`).

## Where this configuration lives

Exact paths depend on the **product and version**; always verify in current documentation.

### Cursor

- **Cursor Settings → MCP**: add or edit servers in the UI (recommended for first-time setup).
- Project or user config files: commonly under **`.cursor/`** in the project or your user config directory—check **Cursor → Settings → MCP → Open config** (or equivalent) for the authoritative path on your build.
- After edits, **reload MCP** or restart Cursor so new servers register.

### Gemini / Google agent tooling

For **Gemini CLI** (and related Google agent tooling that reads the same config), you can define MCP servers in your **user settings file**:

**`~/.gemini/settings.json`**

Put the same `mcpServers` structure there (each server’s `command`, `args`, and `env`) as in the samples above. If your installation uses a wrapper like `selectedAuthType` or a top-level `security` block, follow the shape your **Gemini CLI version** documents—keys and nesting can change between releases, so check **`gemini help`** / official docs if something does not load.

Keep secrets out of shared dotfile backups or synced folders if you paste tokens directly; prefer environment variables and references your client resolves at runtime.

### Other IDEs / runners

Any client that implements MCP can use the same server definitions; only the **config file location and env substitution rules** differ.

## Quick verification

1. **GitHub (Enterprise):** From the agent, ask something read-only against **`github.infra.cloudera.com`**, e.g. “list open PRs in `cloudbreak/cloudbreak`” or “get PR checks.”
2. **Jira:** Ask for a known issue key (e.g. `CB-12345`) and confirm title/status return.
3. **AWS:** Ask for something read-only and region-scoped, e.g. “list EC2 instances in `us-west-2`” or “run `sts get-caller-identity`.”
4. **Azure:** After **`az login`** and **`az account set`**, ask for something read-only, e.g. “list storage accounts in my subscription” or “show current `az account`.”

If a server fails to start, check the client’s **MCP logs** for Docker errors (image pull, `--env-file` path, or missing `-e` passthrough), or 401/403 (bad token or wrong API host). For **Azure MCP in Docker**, confirm the variables in your **`azure-mcp.env`** match what **EnvironmentCredential** expects; for ad-hoc Azure CLI checks, confirm **`az account show`** as in **`cb-cloud-providers`**.

## Repo skills that benefit from MCP

- **`cb-jira`**: Jira + Confluence conventions (when configured); see also `WORKFLOW.md` for the Jira-to-PR flow.
- **`cb-cloud-providers`**: Read-first AWS/Azure/GCP checks; **AWS MCP** and **Azure MCP** align with the same credential flows as **AWS CLI** and **Azure CLI** when configured as above.

See also `WORKFLOW.md` in this directory for end-to-end issue-to-PR flow.
