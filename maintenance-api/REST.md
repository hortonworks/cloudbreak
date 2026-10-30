# Maintenance window schedule REST API

Customer-facing schedule lifecycle for CDP Maintenance Window. Authorization is enforced per scope; see [AUTH.md](AUTH.md).

**Environment embed (Cloudbreak environment service):** optional `maintenanceWindowSchedule` on `EnvironmentV1Request` / `EnvironmentEditV1Request`, `clearMaintenanceWindowSchedule` on edit, and `maintenanceWindowSchedule` on `DetailedEnvironmentV1Response` are implemented and documented on the **Cloudbreak** Environment API (`environment/build/openapi/environment.json`; routed as `/environment/api/...` where the control plane exposes it). That path is **not** the same as the Thunderhead **public** Environments API that the `cdp environments …` CLI calls — `environment-public-api-client` in this repo does **not** yet define or forward `maintenanceWindowSchedule`, so production `cdp environments create-environment` / `edit-environment` / `describe-environment` with this field is **not** end-to-end verified from Cloudbreak alone. Until public API + CLI models land (outside this repository), use the **Maintenance Window** schedule paths below (signed `…/maintenance/api/…`, `scopeType=ENVIRONMENT`) for customer automation; use Environment create/edit/describe with schedule fields only against deployments where you control the Environment service contract (for example local CBD).

## Base URL

```
https://{cdp-endpoint}/maintenance/api
```

`{cdp-endpoint}` is the regional CDP control-plane host (Thunderhead API gateway), for example `consolecp.us-west-1.cdp.cloudera.com`. Paths are served behind that gateway like other CDP services (`/environment/api`, `/cloudbreak/api`, …).

## Authentication

**Public callers (customers, Console, automation):** authenticate with **CDP access-key request signing** (ECDSA), the same mechanism the **`cdp` CLI** and CDP Java/Go clients use for other control-plane APIs. Send signed requests to `https://{cdp-endpoint}/maintenance/api/...`. The gateway validates the signature. Clients must not send `x-cdp-actor-crn` on the public path; the gateway overwrites it with the authenticated principal.

**In-cluster / service-to-service / local dev only:** when no gateway performs request signing (for example maintenance port-forwarded or running under Docker Compose, or another CB service calling maintenance over the internal network), callers may set `x-cdp-actor-crn` directly so authorization can resolve the actor. That pattern is for engineers and automated tests, not for external integrations against production CDP endpoints. Integration tests that hit services directly can use `integrationtest.cleanup.directCrnHeader` (see `CleanupClientUtil` in `integration-test`).

OpenAPI: `{base}/openapi.json` on a running maintenance pod, or the published spec from the maintenance service build (`maintenance/build/openapi/maintenance.json`).

**Public API catalog:** PR CI runs `integration-test/scripts/openapi-check.sh` against specs built by each module's `OpenApiGenerator` test. Release builds upload JSON to S3 (`maintenance-swagger`, `cloudbreak-swagger/maintenance/` — see `maintenance/scripts/build-dev.sh` and `integration-test/scripts/service-registry.sh`). Downstream CDP catalog/SDK generation consumes those artifacts; there is no separate catalog step in this repo.

## Operations

| Method | Path | `operationId` | Success |
|--------|------|---------------|---------|
| GET | `/v1/maintenance/schedules` | `listMaintenanceWindowSchedulesV1` | 200 |
| GET | `/v1/maintenance/schedules/scope/{scopeType}/scopeId/{scopeId}` | `getMaintenanceWindowScheduleV1` | 200 / 404 |
| POST | `/v1/maintenance/schedules` | `createMaintenanceWindowScheduleV1` | 201 / 409 |
| PATCH | `/v1/maintenance/schedules/scope/{scopeType}/scopeId/{scopeId}` | `updateMaintenanceWindowScheduleV1` | 200 / 404 |
| DELETE | `/v1/maintenance/schedules/scope/{scopeType}/scopeId/{scopeId}` | `deleteMaintenanceWindowScheduleV1` | 204 / 404 |
| POST | `/v1/maintenance/schedules/scope/{scopeType}/scopeId/{scopeId}/skip-next` | `skipNextMaintenanceWindowV1` | 200 / 400 / 404 / 409 |
| DELETE | `/v1/maintenance/schedules/scope/{scopeType}/scopeId/{scopeId}/skip-next` | `cancelSkipNextMaintenanceWindowV1` | 200 / 404 |

`scopeType` is a string: `TENANT`, `ENVIRONMENT`, `DATAHUB`, `DATALAKE`, or `FREEIPA`. For `TENANT`, `scopeId` must equal the account id; for other scopes, `scopeId` is the resource CRN.

List filters: optional query `scopeType` and `scopeId` must both be set or both omitted; mismatched pairs return **400**.

## Examples (environment scope)

The snippets below call maintenance **directly** with a manual `x-cdp-actor-crn` header. Use that only for **local or port-forwarded** maintenance (no Thunderhead signing in front). Against a real `{cdp-endpoint}`, use **signed** requests (`cdp` CLI, CDP SDK, or the same access-key signing flow as other control-plane APIs) — do not paste an actor CRN into curl against `consolecp…`.

Create a weekly window at **ENVIRONMENT** scope (same as optional `maintenanceWindowSchedule` on Environment create/edit):

```bash
export MAINTENANCE_BASE="http://localhost:8093/maintenance/api"   # local / port-forward only
export ACTOR_CRN="crn:altus:iam:us-west-1:12345678:user:1000" # dev only: not for public consolecp calls
export ENV_CRN="crn:cdp:environments:us-west-1:12345678:environment:my-env"

curl -sS -X POST "${MAINTENANCE_BASE}/v1/maintenance/schedules" \
  -H "Content-Type: application/json" \
  -H "x-cdp-actor-crn: ${ACTOR_CRN}" \
  -d '{
    "name": "env-sunday-window",
    "scopeType": "ENVIRONMENT",
    "scopeId": "'"${ENV_CRN}"'",
    "recurrenceKind": "WEEKLY",
    "timezone": "America/Los_Angeles",
    "durationMinutes": 120,
    "startLocalTime": "02:00",
    "dayOfWeek": "SUNDAY"
  }'
```

Get the schedule:

```bash
curl -sS "${MAINTENANCE_BASE}/v1/maintenance/schedules/scope/ENVIRONMENT/scopeId/${ENV_CRN}" \
  -H "x-cdp-actor-crn: ${ACTOR_CRN}"
```

Skip the next occurrence:

```bash
curl -sS -X POST \
  "${MAINTENANCE_BASE}/v1/maintenance/schedules/scope/ENVIRONMENT/scopeId/${ENV_CRN}/skip-next" \
  -H "Content-Type: application/json" \
  -H "x-cdp-actor-crn: ${ACTOR_CRN}" \
  -d '{"reason": "Change freeze"}'
```

Delete (archives the schedule):

```bash
curl -sS -X DELETE \
  "${MAINTENANCE_BASE}/v1/maintenance/schedules/scope/ENVIRONMENT/scopeId/${ENV_CRN}" \
  -H "x-cdp-actor-crn: ${ACTOR_CRN}"
```

## CLI and Console

The **`cdp` CLI** and Thunderhead **public** Environments API are maintained **outside** Cloudbreak. This repository ships:

- **Maintenance Window** OpenAPI (`maintenance/build/openapi/maintenance.json`) — suitable for new CLI commands or SDK clients against `{cdp-endpoint}/maintenance/api` (once gateway routing is enabled in your release).
- **Cloudbreak Environment** OpenAPI with embedded schedule fields — consumed by the environment **microservice**, not automatically by `cdp environments …`.

**What is verified in this repo:** signed (or local direct) calls to **`/maintenance/api/v1/maintenance/schedules`** for ENVIRONMENT scope; Environment service create/edit/describe when callers hit **`/environment/api/v1/env/...`** with schedule fields (see `EnvironmentDescription` maintenance-window notes).

**What is not verified here:** `cdp environments describe-environment`, `create-environment`, or `edit-environment` with `maintenanceWindowSchedule` / `clearMaintenanceWindowSchedule` against a regional `consolecp…` endpoint — the CLI targets the public Environments API, which must add the fields and forward them to Cloudbreak before JSON in `--cli-input-json` works. Do **not** document that flow as supported until that Thunderhead/CLI work merges. **Console** UIs that call only the public Environments API have the same gap until then.

**Practical options today**

| Goal | Approach |
|------|----------|
| Env-scoped schedule in **production** automation | Signed POST/PATCH/GET/DELETE on **`…/maintenance/api/v1/maintenance/schedules`** with `scopeType=ENVIRONMENT` and `scopeId=<environment CRN>` (table above). |
| Schedule via **Environment** API | Cloudbreak Environment v1 create/edit/describe (local CBD or any deployment where you call `/environment/api` directly). |
| **`cdp` CLI** | Wait for CLI + public API models; or add dedicated `cdp maintenance-window-schedule` (or similar) against `maintenance.json` in the CLI repo. |

**Verify swagger locally (same gate as PR CI):** with Docker and repo credentials configured:

```bash
cd integration-test
./scripts/build-swagger.sh   # or build only :maintenance:test --tests=...OpenApiGenerator
INTEGCB_LOCATION="$PWD/scripts" CB_VERSION=<line>-b1 CB_TARGET_BRANCH=master ./scripts/openapi-check.sh
```
