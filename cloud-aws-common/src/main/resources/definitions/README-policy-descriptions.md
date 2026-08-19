# AWS credential policy permission descriptions

IAM policy documents under this directory are valid JSON for AWS APIs and cannot include comments or per-action metadata. Human-readable explanations live in parallel **description sidecars**:

| IAM policy | Description sidecar |
|------------|---------------------|
| `aws-environment-minimal-policy.json` | `aws-environment-minimal-policy-descriptions.yaml` |
| `aws-cb-policy.json` | `aws-cb-policy-descriptions.yaml` |
| `aws-gov-cb-policy.json` | `aws-gov-cb-policy-descriptions.yaml` |
| `aws-gov-environment-minimal-policy.json` | `aws-gov-environment-minimal-policy-descriptions.yaml` |

## Sidecar format

```yaml
policyFile: aws-environment-minimal-policy.json   # must match the IAM JSON file name
title: Short human title
summary: When this policy is used and any important scope notes (tags, GovCloud, etc.)
permissions:
  "service:Action": What CDP uses this permission for during onboarding or operations.
```

- Keys under `permissions` must match every `Action` in the IAM policy (including wildcards such as `ec2:*`).
- Always quote permission keys (for example `"ec2:RunInstances"`). IAM actions always use the `service:operation` form, and YAML treats `:` as the key/value separator unless the key is quoted.
- When adding or removing actions in the `.json` file, update the matching `-descriptions.yaml` entry.
- Descriptions should explain **what Cloudbreak uses each permission for** (credential prerequisites, connectors, CloudFormation stacks), not generic AWS API behavior.

## Validation

`AwsPolicyPermissionDescriptionsTest` fails the build if a policy action is missing from its sidecar or if the sidecar lists extra actions.

## Runtime

Cloudbreak still loads and serves the `.json` policies unchanged (base64-encoded via credential prerequisites). Description files are documentation and onboarding aids until exposed through a future API.
