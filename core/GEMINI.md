# Core Module Mandates

This is the central orchestration module for Cloudbreak.

## 🏛 Architecture
- **Application**: `com.sequenceiq.cloudbreak.CloudbreakApplication`.
- **Context Path**: `/cb`.
- **Primary Database**: `cbdb`.

## 📂 Navigation Shortcuts
- **SQL Schema**: `core/src/main/resources/schema` (managed via `cbd migrate cbdb ...`).
- **Blueprints**: `core/src/main/resources/defaults/blueprints/*.bp`.
- **Cloud Templates**: `core/src/main/resources/defaults/clustertemplates/*.json`.

## ⚖ Execution Policies
- **DB Migration**: Scripts must include an `@UNDO` section and follow MyBatis Migrations standards.
- **Node ID**: Every local run must specify `-Dinstance.node.id=CB-1` for flow restart logic to work.
- **Vault**: Secrets are handled via the engine `com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine`.
