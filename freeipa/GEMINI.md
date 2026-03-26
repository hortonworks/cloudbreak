# FreeIPA Module Mandates

This module manages Identity management and DNS for provisioned clusters.

## 🏛 Architecture
- **Application**: `com.sequenceiq.freeipa.FreeIpaApplication`.
- **Context Path**: `/freeipa`.
- **Primary Database**: `freeipadb`.

## 📂 Navigation Shortcuts
- **SQL Schema**: `freeipa/src/main/resources/schema` (managed via `cbd migrate freeipadb ...`).

## ⚖ Execution Policies
- **Service Port**: Default is `8090`.
- **Integration**: Heavily interacts with the `cloud` module for infrastructure provisioning.
