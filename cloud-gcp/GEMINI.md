# GCP Cloud Provider Mandates

This module provides the GCP (Google Cloud Platform) infrastructure integration for Cloudbreak.

## 🛠 SDK & Integration
- **SDK**: Google Cloud Java Client Libraries.
- **Core Connector**: `com.sequenceiq.cloudbreak.cloud.gcp.GcpConnector`.
- **Authentication**: `GcpAuthenticator` handles Service Account authentication.

## 📂 Key Components
- **Resource Builders**: `AbstractGcpResourceBuilder` and its implementations in `group/`, `network/`, `sql/`.
- **Networking**: `GcpNetworkConnector` and `GcpNetworkInterfaceProvider`.
- **Compute**: `GcpInstanceConnector` and `GcpInstanceProvider`.
- **Metadata**: `GcpMetadataCollector` for retrieving instance and network metadata.

## ✅ Operational Mandates & Conventions
1. **Resource Builders**: Use the `ResourceBuilder` pattern (implementing `GcpResourceBuilder`) for consistent resource lifecycle management (create, delete, poll).
2. **Operations**: Use `GcpPoller` and `GcpClient` to manage and wait for GCP asynchronous operations.
3. **Tagging/Labels**: Ensure consistent application of labels via `GcpTagValidator`.
4. **Regionality**: Properly handle Zone vs Region scoped resources (e.g., `GcpAvailabilityZoneConnector`).
5. **Error Handling**: Map GCP `GoogleJsonResponseException` to internal cloud exceptions using `GcpResourceException`.
