# Externalized Compute Module Mandates

This module manages compute clusters external to the core CDP environment.

## 🏛 Architecture
- **Application**: `com.sequenceiq.externalizedcompute.ExternalizedComputeClusterApplication`.
- **Context Path**: `/externalizedcompute`.
- **Primary Database**: `externalizedcomputedb`.

## 📂 Navigation Shortcuts
- **SQL Schema**: `externalized-compute/src/main/resources/schema` (managed via `cbd migrate externalizedcomputedb ...`).

## ⚖ Execution Policies
- **Service Port**: Default is `8091`.
- **Logic**: Orchestrates compute resources on cloud provider managed Kubernetes (EKS, AKS, GKE).
