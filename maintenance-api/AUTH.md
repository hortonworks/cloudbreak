# Maintenance Window API authorization

Public schedule APIs enforce authorization. Internal task APIs under `/internal/maintenance-tasks` are **`@InternalOnly`** (service identity / internal actor CRN only; end users receive 403).

## Schedule and skip (`/v1/maintenance/schedules`)

| Operation | TENANT scope | ENVIRONMENT scope | DATAHUB / DATALAKE / FREEIPA scope |
|-----------|--------------|-------------------|-----------------------------------|
| Read (list/get) | Power user **or** environment creator (account) | `describeEnvironment` on `scopeId` | See resource row below |
| Write (create/update/delete/skip) | Power user (tenant admin) | `editEnvironment` on `scopeId` | See resource row below |

### Resource scope UMS rights (scopeId = resource CRN)

| Scope type | Read | Write |
|------------|------|-------|
| DATAHUB | `describeDatahub` | `upgradeDatahub` (`UPGRADE_DATAHUB` / `datahub/upgradeDatahub`) |
| DATALAKE | `describeDatalake` | `modifyDatalake` |
| FREEIPA | Any of `repairFreeIPA`, `adminFreeIPA`, `describeEnvironment` on `scopeId` | Any of `adminFreeIPA`, `editEnvironment` on `scopeId` |

`scopeId` validation (`MaintenanceWindowScheduleScopeValidator`): TENANT requires `scopeId == accountId`; all other scopes
require a CRN belonging to the caller's account. Violations are **400**, not 403 — a non-CRN `scopeId` would otherwise
trip the UMS client's CRN assertion and surface as a 500.

Unscoped **list** returns only schedules the caller may read. Resource scopes are resolved with one batched UMS
`hasRights` call per distinct right rather than one call per schedule.

### Known issues

- **FREEIPA scope authorization is not expected to succeed.** The three rights above are all `ENVIRONMENT`-typed and
  the FreeIPA service itself evaluates them against an **environment** CRN
  (`FreeIpaV1Controller`: `@CheckPermissionByRequestProperty(path = "environmentCrn", …)`), but a FREEIPA-scoped
  schedule's `scopeId` is a **FreeIPA** CRN (`MaintenanceTaskResourceScope`, `MaintenanceWindowDispatchTickService`).
  UMS will therefore deny FREEIPA-scope reads and writes. Fixing this requires resolving the FreeIPA resource's parent
  environment CRN, which this module cannot currently do: a FreeIPA CRN embeds a random UUID, not the environment, and
  `maintenance` has no environment or freeipa service client. Use ENVIRONMENT scope until this is resolved.

## Internal task APIs (`/internal/maintenance-tasks`)

Register, list, update, delete tasks and report run outcomes: **`@InternalOnly`** with `@AccountId` (or `@RequestObject` with `@ResourceCrn` on peer services). Not callable by interactive users.

Implementation: `MaintenanceWindowScheduleAuthorizationService`, `MaintenanceWindowScheduleController` (`@CustomPermissionCheck`).
