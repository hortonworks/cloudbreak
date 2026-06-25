# Azure credential role definition descriptions

Parallel `*-descriptions.yaml` sidecars document each permission in the Azure custom role JSON files used for **environment** credential prerequisites. Runtime still serves the original JSON unchanged (base64 via `AzureCredentialConnector`).

| Role JSON | Description sidecar | Exposed as |
|-----------|---------------------|------------|
| `azure-role-def.json` | `azure-role-def-descriptions.yaml` | `roleDefinitionJson` in credential prerequisites |
| `azure-minimal-role-def.json` | `azure-minimal-role-def-descriptions.yaml` | `MinimalRoleDefinition` granular policy |

Audit and other role definitions (`azure-audit-role-def.json`, etc.) are out of scope unless a ticket adds sidecars for them.

## Sidecar format

```yaml
policyFile: azure-role-def.json
title: Human-readable title
summary: How Cloudbreak uses this role in credential prerequisites
permissions:
  "Microsoft.Compute/virtualMachines/read": "Cloudbreak-specific description."
```

- Quote every permission key (colons and slashes).
- Include both `Actions` and `DataActions` from the role JSON under `permissions`.
- Keep keys in sync with the JSON; `AzurePolicyPermissionDescriptionsTest` fails on any mismatch.

## Validation

```bash
./gradlew :cloud-azure:test --tests 'com.sequenceiq.cloudbreak.cloud.azure.policy.AzurePolicyPermissionDescriptionsTest'
```
