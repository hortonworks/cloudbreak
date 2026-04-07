# Remote Environment Module Mandates

This module provides services for managing hybrid/remote environments in CDP.

## 🏛 Architecture
- **Application**: `com.sequenceiq.remoteenvironment.RemoteEnvironmentApplication`.
- **Context Path**: `/remoteenvironmentservice`.
- **Primary Database**: `remoteenvironmentdb`.

## 📂 Navigation Shortcuts
- **SQL Schema**: `environment-remote/src/main/resources/schema` (managed via `cbd migrate remoteenvironmentdb ...`).

## ⚖ Execution Policies
- **Service Port**: Default is `8089`.
- **Logic**: Manages registration and health of control planes.
