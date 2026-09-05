# Azure Cloud Provider Mandates

This module provides the Azure infrastructure integration for Cloudbreak.

## 🛠 SDK & Integration
- **SDK**: Azure SDK for Java (com.azure).
- **Core Connector**: `com.sequenceiq.cloudbreak.cloud.azure.AzureConnector`.
- **Authentication**: `AzureAuthenticator` handles Service Principal and Managed Identity authentication.

## 📂 Key Components
- **Resource Management**: `AzureResourceConnector` and `AzureCloudResourceService` manage ARM (Azure Resource Manager) deployments.
- **Templates**: `AzureTemplateBuilder` and `AzureDatabaseTemplateProvider` handle ARM template generation.
- **Networking**: `AzureNetworkConnector` and `AzureDnsZoneService`.
- **Storage**: `AzureStorage` and `AzureStorageAccountService`.

## ✅ Operational Mandates & Conventions
1. **ARM Templates**: Prefer ARM template-based deployments (`AzureResourceConnector`) for infrastructure setup.
2. **Resource Groups**: Follow project conventions for resource group lifecycle management.
3. **Error Handling**: Use `AzureUtils` for common error parsing and retry logic.
4. **Availability Zones**: Use `AzureAvailabilityZoneConnector` to handle regional AZ differences.
5. **Private Links**: Be mindful of `AzureNetworkLinkService` when dealing with private network connectivity.
6. **TLS Hardening**: The shared `okhttp3.OkHttpClient` used by every Azure SDK manager and `TokenCredential` is built inside `AzureHttpClientConfigurer`. Set `cb.azure.tlsHardening=true` (default `false`) to restrict the OkHttp `ConnectionSpec` to TLS 1.3 with the cipher list from `EncryptionProfileProvider.getTls13RecommendedCipherSuites(true)`.
