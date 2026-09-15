# Azure credential role definition descriptions

Parallel `*-descriptions.yaml` sidecars document each permission in the Azure custom role JSON files used for **environment** credential prerequisites. Runtime still serves the original JSON unchanged (base64 via `AzureCredentialConnector`).

| Role JSON | Description sidecar | Exposed as |
|-----------|---------------------|------------|
| `azure-role-def.json` | `azure-role-def-descriptions.yaml` | `roleDefinitionJson` in credential prerequisites |
| `azure-minimal-role-def.json` | `azure-minimal-role-def-descriptions.yaml` | `MinimalRoleDefinition` granular policy |

The following additional sidecars describe the two resource-group-scoped policies in the
[Azure provisioning credential documentation](https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-azure/topics/mc-azure-credential.html):

| Documented role | Description sidecar | Policy snapshot (`src/test/resources/definitions`) |
|-----------------|---------------------|----------------------------------------------------|
| Role definition 1: service endpoints | `azure-service-endpoints-role-def-descriptions.yaml` | `azure-service-endpoints-role-def.json` |
| Role definition 2: private endpoints | `azure-private-endpoints-role-def-descriptions.yaml` | `azure-private-endpoints-role-def.json` |

For these two files, `policyFile` refers to a test snapshot of the JSON definition from the documentation linked above.
When the documented policy changes, update the snapshot and its sidecar together. These snapshots are not runtime policies.

Audit and other role definitions (`azure-audit-role-def.json`, etc.) are out of scope unless a ticket adds sidecars for them.

## Sidecar format

```yaml
policyFile: azure-role-def.json
title: Human-readable title
summary: How Cloudbreak uses this role in credential prerequisites
permissionGroups:
  compute:
    description: Required to create and manage cluster VMs, images, and disks.
    permissions:
      "Microsoft.Compute/virtualMachines/read": What Cloudera uses this permission for.
```

- Quote every permission key (colons and slashes).
- Include both `Actions` and `DataActions` from the role JSON under `permissions`.
- Keep keys in sync with the JSON; `AzurePolicyPermissionDescriptionsTest` fails on any mismatch.

## Permission groups

- Each entry under `permissionGroups` has a stable group name, a non-empty `description`, and a `permissions` map.
- Group descriptions explain why Cloudera needs the related resources in the customer's account. Groups represent a shared purpose, not cloud resource groups.
- Every policy permission must occur in exactly one group; retain its individual description under that group's `permissions` map.
- Validation checks group descriptions and membership, rejects duplicate keys, and compares all grouped permissions with the policy JSON.

## Validation

```bash
./gradlew :cloud-azure:test --tests 'com.sequenceiq.cloudbreak.cloud.azure.policy.AzurePolicyPermissionDescriptionsTest'
```
