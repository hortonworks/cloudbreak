# AWS Cloud Provider Mandates

This module provides the AWS infrastructure integration for Cloudbreak.

## 🛠 SDK & Integration
- **SDK**: Standard AWS Java SDK v2.
- **Core Client**: `com.sequenceiq.cloudbreak.cloud.aws.common.AwsClient` - Centralizes AWS client creation and session management.
- **Common Logic**: Located in `cloud-aws-common/`. Specific implementations are in `cloud-aws-cloudformation/`, `cloud-aws-native/`, and `cloud-aws-gov/`.

## 📂 Key Components
- **Connectors**: `AwsInstanceConnector`, `AwsNetworkConnector`, `AwsResourceConnector`.
- **Credential Management**: `AwsCredentialConnector` and `AwsAuthenticator`.
- **Error Handling**: `AwsSdkErrorCodes` handles provider-specific error mapping.
- **Services**: Sub-packages like `efs/`, `kms/`, `loadbalancer/` handle specific AWS services.

## ✅ Operational Mandates & Conventions
1. **Client Reusability**: Always use `AwsClient` to obtain SDK clients to ensure proper regional and credential configuration.
2. **Tagging**: Use `AwsTaggingService` for consistent resource tagging across all AWS resources.
3. **Error Mapping**: Map AWS-specific exceptions to `CloudConnectorException` using the provided mappers.
4. **CloudFormation vs Native**: Prefer `cloud-aws-cloudformation` for complex stacks and `cloud-aws-native` for direct resource manipulation when CF is not suitable.
5. **Regionality**: Always consider regional differences (e.g., GovCloud in `cloud-aws-gov`).
