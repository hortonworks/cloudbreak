# GCP credential policy permission descriptions

GCP custom role documents under this directory are valid JSON for `gcloud iam roles create --file` and cannot include comments or per-permission metadata. Human-readable explanations live in parallel **description sidecars**:

| Custom role JSON | Description sidecar |
|------------------|---------------------|
| `gcp-environment-minimal-policy.json` | `gcp-environment-minimal-policy-descriptions.yaml` |

## Sidecar format

```yaml
policyFile: gcp-environment-minimal-policy.json   # must match the custom role JSON file name
title: Short human title
summary: When Cloudbreak exposes this role and how it is used in credential prerequisites
permissionGroups:
  compute:
    description: Required to create VMs from images and manage cluster hosts.
    permissions:
      "compute.instances.create": What Cloudera uses this permission for.
```

- Keys under `permissions` must match every entry in `includedPermissions` in the custom role JSON.
- Always quote permission keys (for example `"compute.instances.create"`). GCP permissions use dotted `service.resource.verb` names, and YAML treats `:` as the key/value separator unless the key is quoted.
- When adding or removing permissions in the `.json` file, update the matching `-descriptions.yaml` entry.
- Descriptions should explain **what Cloudbreak uses each permission for** (for example `GcpCredentialConnector` minimal prerequisites and GCP resource builders), not generic GCP API behavior.

## Permission groups

- Each entry under `permissionGroups` has a stable group name, a non-empty `description`, and a `permissions` map.
- Group descriptions explain why Cloudera needs the related resources in the customer's account. Groups represent a shared purpose, not cloud resource groups.
- Every policy permission must occur in exactly one group; retain its individual description under that group's `permissions` map.
- Validation checks group descriptions and membership, rejects duplicate keys, and compares all grouped permissions with the policy JSON.

## Validation

`GcpPolicyPermissionDescriptionsTest` fails the build if a permission is missing from its sidecar or if the sidecar lists extra entries.

## Runtime

Cloudbreak still loads and serves the `.json` role definition unchanged (base64-encoded as `MinimalPrerequisitesCreationPermissions` via credential prerequisites). Description files are documentation and onboarding aids until exposed through a future API.
