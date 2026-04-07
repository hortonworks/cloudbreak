# Datalake Module Mandates

This module manages the SDX (Shared Data Experience) lifecycle.

## 🏛 Architecture
- **Application**: `com.sequenceiq.datalake.DatalakeApplication`.
- **Context Path**: `/dl`.
- **Primary Database**: `datalakedb`.

## 📂 Navigation Shortcuts
- **SQL Schema**: `datalake/src/main/resources/schema` (managed via `cbd migrate datalakedb ...`).
- **Logic**: Flow-based orchestration using the `flow` module patterns.

## ⚖ Execution Policies
- **Node ID**: Every local run must specify `-Dinstance.node.id=CB-1` for flow restart logic.
- **Service Port**: Default is `8086`.
