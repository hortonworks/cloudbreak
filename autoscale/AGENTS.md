# Autoscale (Periscope) Module Mandates

This module manages the automatic scaling of Cloudbreak clusters based on policies.

## 🏛 Architecture
- **Application**: `com.sequenceiq.periscope.PeriscopeApplication`.
- **Context Path**: `/as`.
- **Primary Database**: `periscopedb`.

## 📂 Navigation Shortcuts
- **SQL Schema**: `autoscale/src/main/resources/schema` (managed via `cbd migrate periscopedb ...`).

## ⚖ Execution Policies
- **Service Port**: Default is `8085`.
- **Integration**: Monitors Cloudera Manager metrics and triggers scaling flows in Cloudbreak (Core).
