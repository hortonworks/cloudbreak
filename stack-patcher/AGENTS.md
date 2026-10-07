# Stack Patcher Module Mandates

This module provides the existing stack patcher framework: a Quartz job per (resource, `StackPatchType`) that detects whether an already-provisioned resource is affected by a known issue and applies a fix. Services adopt it by implementing `StackPatchResourceService`.

## Core Concepts
- **`StackPatchType`**: Single shared enum for all services. Mark retired types `DEPRECATED` instead of removing them, because rows in the database still reference them.
- **`ExistingStackPatchService<R>`**: One subclass per patch type. Implements `getStackPatchType()`, `isAffected(R)` and `doApply(R)`. The base class's `apply` skips while another flow is running or a retryable failed flow exists (override `shouldCheckForFailedRetryableFlow()` to opt out) and wraps unexpected errors in `ExistingStackPatchApplyException`.
- **`StackPatchResourceService<R>`**: Implemented once per service. Tells the framework how to load, filter and schedule that service's resource.
- **`StackPatchResource`** (`common`): What the framework needs from the resource: `getId()` and `getResourceCrn()`.
- **`StackPatchUsageReporterService`**: Sends a `CDPStackPatchEvent` usage event for reported status changes (`AFFECTED` once, `FIXED`, `FAILED`) of every adopter. Patch services can call `reportUsage` directly for custom events.
- **`stackpatch` table**: One row per (`resource_id`, `type`) recording `StackPatchStatus`. It has no FK to the resource table.

For the step-by-step procedure for adding a patch, or adopting the module in a new service, see the **cb-stack-patcher** skill (`.agent/skills/cb-stack-patcher/SKILL.md`).

## Key Entry Points
- **Job**: `com.sequenceiq.stackpatcher.job.ExistingStackPatcherJob`, scheduled at startup by `ExistingStackPatcherJobInitializer`.
- **Configuration**: `com.sequenceiq.stackpatcher.config.ExistingStackPatcherConfig` (`existing-stack-patcher.*` in each service's `application.yml`).
- **Schema**: `stack-patcher/src/main/resources/schema/stackpatch/`, registered by `StackPatchSchemaLocationProvider`.
## Execution Rules
- **Short jobs**: `doApply` runs inside a Quartz job. For long or multi-step work, trigger a flow and return `true` once it has been accepted, rather than blocking.
- **Return values**: `false` from `doApply` means "not now". The patch becomes `SKIPPED` and is retried on the next interval. Throw `ExistingStackPatchApplyException` only for real failures; those mark the patch `FAILED` and are reported.
- **Cleanup**: There is no FK, so every adopter must call `StackPatchService.deleteByResourceId(id)` when it terminates or hard-deletes the resource.
- **Schema changes**: Changes to the `stackpatch` table go in `stack-patcher/src/main/resources/schema/stackpatch/` so that every adopter receives them. Do not add per-service `stackpatch` migrations.
- **Retiring a patch**: Mark the type `DEPRECATED`, delete its service, and set `enabled: false` in config. Each service's `StackPatchConfigApplicationTest`-style test should assert that deprecated types stay disabled.

## Testing
- Framework tests live in this module and use `TestStackPatchResource` as the resource fixture.
- Patch services are unit tested in the adopting service, with the framework collaborators mocked.
