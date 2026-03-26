# Environment Module Mandates

This module manages CDP environments across different cloud providers.

## 🏛 Architecture
- **Application**: `com.sequenceiq.environment.EnvironmentApplication`.
- **Context Path**: `/environmentservice`.
- **Primary Database**: `environmentdb`.

## 📂 Navigation Shortcuts
- **SQL Schema**: `environment/src/main/resources/schema` (managed via `cbd migrate environmentdb ...`).
- **Logic**: Orchestrates network, storage, and authentication resources for cloud environments.

## ⚖ Execution Policies
- **Service Port**: Default is `8088`.
- **Cloud Support**: Supports AWS, Azure, GCP, and Mock providers.
