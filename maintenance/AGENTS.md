# Maintenance Module Mandates

This module is the dedicated Maintenance Window service for CDP.

## Architecture
- **Application**: `com.sequenceiq.maintenance.MaintenanceApplication`.
- **Context Path**: `/maintenance`.
- **Primary Database**: `maintenancedb`.

## Navigation Shortcuts
- **SQL Schema**: `maintenance/src/main/resources/schema` (managed via `cbd migrate maintenancedb ...`).
- **Logic**: Stores maintenance schedules, task registrations, and dispatches gated platform automation during allowed windows.

## Execution Policies
- **Service Port**: Default is `8093`.
- **Dispatcher**: Quartz job polls ACTIVE tasks on a configurable interval (default 15 minutes).
