# Redbeams Module Mandates

This module manages external database as a service (DBaaS) for CDP.

## 🏛 Architecture
- **Application**: `com.sequenceiq.redbeams.RedbeamsApplication`.
- **Context Path**: `/redbeams`.
- **Primary Database**: `redbeamsdb`.

## 📂 Navigation Shortcuts
- **SQL Schema**: `redbeams/src/main/resources/schema` (managed via `cbd migrate redbeamsdb ...`).

## ⚖ Execution Policies
- **Service Port**: Default is `8087`.
- **Logic**: Orchestrates DB instance creation on AWS (RDS), Azure, and GCP.
