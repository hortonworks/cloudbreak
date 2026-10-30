# Maintenance Window API authorization

Public schedule APIs enforce authorization. Internal task APIs under `/internal/maintenance-tasks` are **`@InternalOnly`** (service identity / internal actor CRN only; end users receive 403).
REST paths, curl examples, and stable `operationId` values for the public schedule API are in [REST.md](REST.md). Environment create/edit/describe embed the same schedule for `ENVIRONMENT` scope (see Environment OpenAPI / `EnvironmentModelDescription`). Interactive callers use `/v1/maintenance/schedules`; internal APIs are `@InternalOnly` at runtime — schedule lookup under `/internal/maintenance-schedules` and task registration under `/internal/maintenance-tasks` (see below).

## Feature entitlement

All public schedule operations (list, get, create, PATCH, delete, skip-next, cancel-skip, and when present preview/effective) require the account entitlement **`CDP_MAINTENANCE_WINDOW`**. When the entitlement is missing, the API returns **403 Forbidden** with a message naming the entitlement. IAM checks below apply only after the entitlement passes.

## Schedule and skip (`/v1/maintenance/schedules`)

IAM uses existing CDP resource rights; there are no dedicated maintenance-window UMS rights. Mapping:

| Operation | TENANT scope | ENVIRONMENT scope | DATAHUB / DATALAKE / FREEIPA scope |
|-----------|--------------|-------------------|-----------------------------------|
| Read (list/get/effective/preview) | Power user **or** environment creator (account) | `describeEnvironment` on `scopeId` | See resource row below |
| Write (create/update/delete/skip) | Power user (tenant admin) | `editEnvironment` on `scopeId` | See resource row below |

**Preview** (`POST …/preview`): when the body omits `scopeType`/`scopeId`, TENANT read on the account applies; when scope
fields are set, the same read rule applies at that scope (persisted skip-next uses the scoped schedule).

**Effective** (`GET …/effective?resourceCrn=…`): read access on the **resource** CRN (derived scope type) is always
required first. After the effective schedule is resolved, any further check depends on the **winning source**:

- **ENVIRONMENT-sourced** result: **describeEnvironment** on the parent environment CRN is additionally required.
- **TENANT-sourced** result: **no** additional right — resource read alone is sufficient. A caller with read only on a
  Data Hub still sees the inherited tenant-scoped schedule and is **not** required to hold tenant read.
- **Resource-sourced** result (the resource's own scope wins): already covered by the resource read above; no extra check.

If the resource is unknown to the owning service, environment inheritance is skipped and only resource and tenant
schedules are considered. An optional `environmentCrn` query parameter is rejected when it does not match the resolved
parent environment, or when a parent environment cannot be resolved.

### Resource scope UMS rights (`scopeId` = resource CRN unless noted)

| Scope type | UMS resource CRN used | Read (any one for FREEIPA) | Write (any one for FREEIPA) |
|------------|----------------------|----------------------------|----------------------------|
| DATAHUB | `scopeId` (Data Hub cluster CRN) | `describeDatahub` | `upgradeDatahub` (`UPGRADE_DATAHUB` / `datahub/upgradeDatahub`) |
| DATALAKE | `scopeId` (Data Lake CRN) | `describeDatalake` | `modifyDatalake` |
| FREEIPA | Parent **environment** CRN resolved from FreeIPA `scopeId` via FreeIPA internal lookup | `repairFreeIPA`, `adminFreeIPA`, or `describeEnvironment` | `adminFreeIPA` or `editEnvironment` |

FREEIPA-scoped schedules store a **FreeIPA resource CRN** in `scopeId`. UMS rights for FreeIPA operations are defined against the **environment** CRN (same as `FreeIpaV1Controller`). The maintenance service resolves the parent environment with `GET /v1/internal/freeipa/environment_crn?resourceCrn=…` (`@InternalOnly`), using **internal actor** identity for that outbound call while UMS checks still use the end user's CRN. If lookup fails, access is denied (**403**).

`scopeId` validation (`MaintenanceWindowScheduleScopeValidator`): TENANT requires `scopeId == accountId`; all other scopes require a CRN belonging to the caller's account. Violations are **400**, not 403 — a non-CRN `scopeId` would otherwise trip the UMS client's CRN assertion and surface as a 500.

Unscoped **list** returns only schedules the caller may read. Resource scopes are resolved with one batched UMS `hasRights` call per distinct right rather than one call per schedule.

## Internal schedule APIs (`/internal/maintenance-schedules`)

Service-to-service schedule lookups and compensation: **`@InternalOnly`** with `@AccountId`. Not callable by interactive users.

| Operation | Purpose |
|-----------|---------|
| `get` | Describe-time schedule lookup under an internal actor (no public UMS check) |
| `restore` (`PUT .../restore`) | Full-state restore by id to compensate a failed cross-service edit; reactivates the existing row (preserving its id and attached skips) and overwrites every field, including nulls |

## Internal schedule lookup (`/internal/maintenance-schedules`)

**GET** `/internal/maintenance-schedules/scope/{scopeType}/scopeId/{scopeId}` with query **`accountId`** (`getMaintenanceWindowScheduleInternal`): **`@InternalOnly`**. The caller must use a service/internal actor; interactive users receive **403**. Scope is supplied explicitly (`accountId` plus path scope); this path does **not** run the public UMS schedule matrix in `MaintenanceWindowScheduleAuthorizationService`. Used for service-to-service reads (for example when the Environment service serves **describe environment**, it loads optional `maintenanceWindowSchedule` onto `DetailedEnvironmentV1Response` through this internal path rather than the public schedule GET).

## Internal task APIs (`/internal/maintenance-tasks`)

Register, list, update, delete tasks and report run outcomes: **`@InternalOnly`** with `@AccountId` (or `@RequestObject` with `@ResourceCrn` on peer services). Not callable by interactive users. Task APIs are not gated by `CDP_MAINTENANCE_WINDOW` (platform automation continues to register work when the entitlement is off).

Implementation: public schedule UMS checks — `MaintenanceWindowScheduleEntitlementService`, `MaintenanceWindowScheduleAuthorizationService`, `MaintenanceWindowScheduleController` (`@CustomPermissionCheck`); internal schedule GET — `MaintenanceWindowScheduleInternalController` (`@InternalOnly`).
