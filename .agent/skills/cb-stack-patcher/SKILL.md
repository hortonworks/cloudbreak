---
name: cb-stack-patcher
description: Add an existing stack patcher (a background fix for already-provisioned resources) or adopt the shared stack-patcher module in a service that has none yet — StackPatchType, ExistingStackPatchService, StackPatchResourceService, config, cleanup. Use when fixing existing stacks in core/freeipa or bringing stack patchers to environment/datalake/redbeams.
---

# Cloudbreak stack patcher — adding a patch or a new adopter

The reference concepts live in **`stack-patcher/AGENTS.md`**. This skill is the **step-by-step procedure**. Use **Adopt the module** only when the service has no `StackPatchResourceService` implementation yet; otherwise go straight to **Add a patch**.

Reference adopters: core (`CloudbreakStackPatchResourceService`) and FreeIPA (`FreeIpaStackPatchResourceService`).

## Adopt the module in a new service

1. **Gradle**: Add `implementation project(':stack-patcher')` to the service's `build.gradle`.
2. **Resource entity**: Make the patched entity implement `StackPatchResource`. It must also be usable as a `MdcContextInfoProvider`, either directly (as FreeIPA's `Stack` does) or through another object returned from `getMdcContextInfoProvider`.
3. **`StackPatchResourceService<R>`**: Add exactly one `@Service` implementation.
   - `loadResource`: Load the entity with everything `isAffected`/`doApply` need, typically inside a transaction.
   - `isUnschedulable`: Return true for terminal or deleted states. The job then marks the patch `UNSCHEDULED` and stops.
   - `findAllSchedulableResources`: Return the `JobResource`s the initializer should schedule at startup. Reuse the service's existing auto-sync query.
   - `getJobResource`, `getMdcContextInfoProvider`: Look up the resource by id.
4. **Resource ids must match flow resource ids.** `ExistingStackPatchService.apply` calls `FlowLogService.isOtherFlowRunning(resource.getId())`, so the patched resource's id must be the one the service's flows run on.
5. **Database**:
   - Nothing to add for the table. `StackPatchSchemaLocationProvider` registers `schema/stackpatch` as a pending migration folder, and the idempotent baseline creates the `stackpatch` table on the next start.
   - If the service builds its own `LocalContainerEntityManagerFactoryBean` with an explicit `setPackagesToScan` list (redbeams, autoscale), add `com.sequenceiq.stackpatcher`. Services using `service-common`'s `DatabaseConfig` already scan `com.sequenceiq`.
6. **Cleanup**: There is no FK, so call `StackPatchService.deleteByResourceId(id)` wherever the service hard-deletes or terminates the resource. Core does this in `StackService.deleteArchivedByResourceCrn`, FreeIPA in `TerminationService`.
7. **Configuration**: Add an `existing-stack-patcher` block to `application.yml`. All three scalar properties are required.
   ```yaml
   existing-stack-patcher:
     interval-in-hours: 24
     max-initial-start-delay-in-hours: 12
     initialization-chunk-size: 100
     patch-configs: {}
   ```
8. **Config test**: Copy core's `StackPatchConfigApplicationTest`, which asserts that `DEPRECATED` types stay disabled in config.

## Add a patch

1. **Type**: Add a value to `StackPatchType` in `stack-patcher`. The enum is global across services: never reuse an existing type for a different fix.
2. **Service**: Add a `@Service` extending `ExistingStackPatchService<YourResource>`:
   - `getStackPatchType()`: return the new type.
   - `isAffected(R)`: cheap and side-effect free. It runs on every job execution until the patch reaches a final status.
   - `doApply(R)`: return `true` when fixed. Return `false` for "not now": the patch becomes `SKIPPED` and is retried on the next interval. Throw `ExistingStackPatchApplyException` only for real failures, which mark the patch `FAILED` and are reported.
   - For long or multi-step work, trigger a flow and return `true` once it has been accepted, rather than blocking the Quartz thread.
   - `AFFECTED`, `FIXED` and `FAILED` are reported as usage events automatically. For extra milestones, call `StackPatchUsageReporterService.reportUsage` with a custom `CDPStackPatchEventType`, as `AwsGp2ToGp3PatchService` does.
   - Override `shouldCheckForFailedRetryableFlow()` to `false` only if the patch doesn't start a flow, or must run even when a retryable failed flow exists.
3. **Config**: Add the type under `existing-stack-patcher.patch-configs` (kebab-case key) in the service's `application.yml`. A type without `enabled: true` is never scheduled.
   ```yaml
   patch-configs:
     my-new-patch:
       enabled: true
       entitlement: CDP_SOME_ENTITLEMENT # optional; gates the patch per account
   ```
4. **Tests**: Unit-test `isAffected` and `doApply` in the adopting service, mocking the framework collaborators. The existing `*PatchServiceTest` classes are good templates.

## Retire a patch

1. Mark the type `DEPRECATED` in `StackPatchType`. Do not remove it, because rows in the database still reference it.
2. Delete its `ExistingStackPatchService` and tests.
3. Set `enabled: false` for it in `patch-configs`. At startup, the initializer purges the rows of `DEPRECATED` types listed as disabled.

## Schema changes

Changes to the `stackpatch` table go in `stack-patcher/src/main/resources/schema/stackpatch/` so that every adopter receives them. Never add a per-service `stackpatch` migration. Follow **cb-db-migration** for naming and `@UNDO`.

## Verify

- `./gradlew :stack-patcher:test :<service>:test --quiet`
- Start the service locally and check:
  - the `changelog` table contains the stack-patcher baseline migration;
  - the log line `Existing stack patch types enabled: [...]` lists your type.
