# Common Module Mandates

This module contains shared utilities and the Quartz execution engine used across all Cloudbreak services.

## 🛠 Shared Utilities
- **JSON Processing**: 
  - `com.sequenceiq.cloudbreak.common.json.Json`: Wrapper for JSON strings.
  - `com.sequenceiq.cloudbreak.common.json.JsonUtil`: Standard Jackson-based utility for serialization/deserialization.
- **Vault Integration**: 
  - `com.sequenceiq.cloudbreak.vault.ThreadBasedVaultReadFieldProvider`: Provides thread-local access to Vault secrets.
- **Cache Management**: 
  - `com.sequenceiq.cloudbreak.cache.CacheDefinition`: Interface for defining Spring-based cache configurations.
- **Logging & MDC**: 
  - `com.sequenceiq.cloudbreak.logger.MDCBuilder`: Centralized utility for building and managing Mapped Diagnostic Context (MDC) for logging.
  - `com.sequenceiq.cloudbreak.logger.MdcContext`: Utility for preserving MDC across thread boundaries.

## ⏰ Quartz Execution Engine
Quartz is used for scheduled background tasks and periodic status checks.

### 🧂 Core Components
- **`MdcQuartzJob`**: Base class for all Quartz jobs. It ensures that MDC context is correctly initialized and preserved during job execution.
- **`JobSchedulerService`**: Utility for scheduling and managing Quartz jobs.
- **`TransactionalScheduler`**: A wrapper around the Quartz `Scheduler` that ensures job scheduling is transaction-aware.
- **`JobInitializer`**: Interface for classes that schedule jobs during application startup.

### ⚖ Execution Rules
1. **Context Preservation**: All Quartz jobs MUST extend `MdcQuartzJob` to ensure traceability in logs.
2. **Transactional Scheduling**: Prefer using `TransactionalScheduler` to ensure that jobs are only scheduled if the surrounding database transaction successfully commits.
3. **Retry Logic**: Be mindful of the retry configuration, especially for cloud provider calls. Use `AzureQuartzRetryUtils` (or similar for other providers) to prevent aggressive retries within Quartz threads.
4. **Job Initializers**: Use `com.sequenceiq.cloudbreak.quartz.model.JobInitializer` to schedule jobs during application startup.

### 🗺 Cross-Project Quartz Usage
Quartz jobs are distributed across modules, following a consistent pattern of `Job`, `JobService` (implementing `JobSchedulerService`), and `JobInitializer` (implementing `JobInitializer`).

| Module | Purpose | Key Jobs / Initializers |
| :--- | :--- | :--- |
| **Core** | Resource sync and cleanup | `StackArchiverJob`, `InstanceCheckerJob`, `ClouderaManagerSyncJob`, `MeteringSyncJob` |
| **Flow** | Flow cleanup | `FlowCleanupJob` |
| **FreeIPA** | Status sync and cleanup | `StructuredSynchronizerJob`, `StackSaltStatusJob`, `ProviderSyncJob` |
| **Datalake** | SDX cluster monitoring | `SdxClusterJobInitializer` |
| **Environment** | Env sync and archiving | `EnvironmentArchiverJob`, `StructuredSynchronizerJob` |
| **Redbeams** | Database stack sync | `DBStackJobInizializer` |
| **Notification** | Notification cleanup | `NotificationCleanupJob` |
| **Secret Rotation**| Periodic rotations | `PeriodicRotationJobInitializer` |
| **Common** | Base job patterns | `StatusCheckerJob`, `UMSCleanupJob`, `SaltStatusCheckerJob` |

## ✅ Operational Mandates & Conventions
1. **Utility First**: Always check for an existing utility in `common/` before implementing custom logic for JSON, Logging, or Caching.
2. **Thread Boundaries**: When spawning new threads, always use `MdcContext.builder()` to propagate the MDC context (e.g., `requestId`, `tenant`, `resourceCrn`).
3. **Secrets**: Use `VaultConstants` and the common Vault utilities to ensure consistent secret handling.
