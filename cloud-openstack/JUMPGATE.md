# OpenStack Jumpgate — Hybrid Cloud Proxy Integration

Enables CDP to manage OpenStack resources in private environments that are not
directly reachable from the public control plane, by routing all OpenStack API traffic
through the CDP cluster proxy (jumpgate agent).

## Overview

The environment connects a CDP Public Cloud environment to a remote environment
identified by `remoteEnvironmentCrn`. The jumpgate agent running in that private
environment provides a secure tunnel. This feature registers OpenStack API endpoints with
the cluster proxy so that Cloudbreak can reach them without direct network connectivity.

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                           CDP Public Cloud                                     │
│                                                                                │
│  ┌──────────────┐    ┌─────────────────┐    ┌───────────────────────────────┐  │
│  │  Cloudbreak  │    │  Environment    │    │        Cluster Proxy          │  │
│  │  (core)      │    │  Service        │    │                               │  │
│  │              ├───►│                 │    │  ┌─────────────────────────┐  │  │
│  │  DL/DH       │    │  remoteEnvCrn ──┼────┼─►│ Registered Services     │  │  │
│  │  provision   │    │                 │    │  │                         │  │  │
│  └──────┬───────┘    └─────────────────┘    │  │  keystone → :5000/v3    │  │  │
│         │                                   │  │  nova     → :8774/v2.1  │  │  │
│         │  register with                    │  │  neutron  → :9696       │  │  │
│         │  remoteEnvironmentCrn             │  │  glance   → :9292       │  │  │
│         ▼                                   │  │  cinder   → :8776/v3    │  │  │
│  ┌──────────────┐                           │  └─────────────┬───────────┘  │  │
│  │  FreeIPA     │                           │                │              │  │
│  │  (no local   │ ─── register with ───────►│                │              │  │
│  │   agent)     │     remoteEnvironmentCrn  └────────────────┼──────────────┘  │
│  └──────────────┘                                            │                 │
│                                                              │                 │
└──────────────────────────────────────────────────────────────┼─────────────────┘
                                                               │
                                           Jumpgate tunnel     │
                                           (outbound from      │
                                            private cloud)     │
                                                               │
┌──────────────────────────────────────────────────────────────┼─────────────────┐
│                       Private Cloud                          │                 │
│                                                              ▼                 │
│  ┌─────────────────┐         ┌──────────────────────────────────────────────┐  │
│  │  Jumpgate Agent │◄────────│              OpenStack APIs                  │  │
│  │  (connects to   │         │                                              │  │
│  │   cluster proxy)│         │  Keystone ─ Nova ─ Neutron ─ Glance ─ Cinder │  │
│  └─────────────────┘         └──────────────────────────────────────────────┘  │
│                                                                                │
└────────────────────────────────────────────────────────────────────────────────┘
```

## Lifecycle

### 1. Credential Creation — Service Registration

When an OpenStack credential is created with `remoteEnvironmentCrn`, the first
`authenticate()` call triggers registration of OpenStack services with the cluster proxy.

```
OpenStackAuthenticator.authenticate()
        │
        ▼
ensureClusterProxyRegistration()
        │
        ├── isRegistered? ──── yes ───► skip (already done)
        │
        no
        │
        ▼
OpenStackClusterProxyService.registerServices()
        │
        ├── 1. Register keystone endpoint with cluster proxy
        │
        ├── 2. Authenticate through proxy (keystone via jumpgate)
        │       └── obtain token + service catalog
        │
        └── 3. Register all discovered services from catalog
                (filtered by credential's "facing" field)
```

### 2. Runtime — URL Rewriting

Once registered, all openstack4j API calls are routed through the proxy using
`JumpgateEndpointURLResolver`:

```
openstack4j call                                Cluster Proxy
─────────────────                               ─────────────
nova.servers().list()
        │
        ▼
JumpgateEndpointURLResolver
  resolves "nova" to:
  http://localhost:10180/cluster-proxy/proxy/{crn}/nova
        │
        ▼
Cluster proxy looks up registered                     Jumpgate Agent
"nova" service endpoint            ───────────────►   forwards to
                                                      https://openstack:8774/v2.1/{tenant}/servers
```

### 3. Environment Creation

When an environment is created with `remoteEnvironmentCrn`:
- FreeIPA skips CCM agent registration (returns empty `DefaultCcmV2JumpgateParameters`)
- FreeIPA registers with cluster proxy using `remoteEnvironmentCrn`, omitting `CcmV2Entries`
- Datalake and DataHub register with cluster proxy using `remoteEnvironmentCrn`
  (not the local environment CRN)

```
Environment
  └── remoteEnvironmentCrn: crn:...:environment:private-env-id
        │
        ├── FreeIPA ClusterProxyService
        │     └── withEnvironmentCrn(remoteEnvironmentCrn)  ← routes via private jumpgate
        │
        ├── Core ClusterProxyService (DL/DH)
        │     └── withEnvironmentCrn(remoteEnvironmentCrn)  ← routes via private jumpgate
        │
        └── CcmUserDataService
              └── skips agent registration (no local jumpgate needed)
```

### 4. Credential Deletion — Cleanup

```
CredentialDeleteService.deleteByName/Crn()
        │
        ▼
ServiceProviderCredentialAdapter.delete()
        │
        ▼
DeleteCredentialHandler → OpenStackCredentialConnector.delete()
        │
        ▼
OpenStackClusterProxyService.deregisterServices()
        │
        ▼
ClusterProxyRegistrationClient.deregisterConfig(clusterCrn)
```

Deregistration failure is logged but does not block credential deletion.

## Key Configuration

### CRN Format

Each OpenStack credential gets a deterministic cluster proxy CRN:

```
crn:cdp:openstack-jumpgate:us-west-1:{accountId}:jumpgate:{credentialName}
```

### Environment Variables

| Property | Default | Description |
|----------|---------|-------------|
| `clusterProxy.url` | `http://localhost:10180/cluster-proxy` | Cluster proxy base URL (prod override in `application-prod.yml`) |
| `clusterProxy.enabled` | `true` | Enable/disable cluster proxy |
| `cb.openstack.disable.ssl.verification` | `false` | Disable SSL for OpenStack calls |

## Affected Modules

| Module | What Changed |
|--------|-------------|
| `cloud-openstack` | Authenticator registers services; `JumpgateEndpointURLResolver` rewrites URLs; `OpenStackClusterProxyService` manages lifecycle |
| `cluster-proxy` | `readConfig` throws `ClusterProxyException` on missing configs; `isRegistered()` in cloud-openstack catches it to detect unregistered state |
| `core` | `ClusterProxyService` uses `remoteEnvironmentCrn` for DL/DH jumpgate registration |
| `freeipa` | `ClusterProxyService` uses `remoteEnvironmentCrn` and omits `CcmV2Entries`; `CcmUserDataService` skips agent registration for remote jumpgate (returns empty `DefaultCcmV2JumpgateParameters`) |
| `environment` | Accepts `remoteEnvironmentCrn` at creation; credential delete calls provider-side cleanup |
| `environment-api` | `remoteEnvironmentCrn` on `OpenstackParameters`, `EnvironmentRequest` |
| `cloud-reactor` | New `DeleteCredentialHandler` |
| `sdx-connector` | `AbstractPdlSdxService` uses `EnvironmentType` enum for type check |

## Authorization

When `remoteEnvironmentCrn` is set on a credential create or modify request:

1. **Same-account check** — the CRN must belong to the caller's account
2. **EnvironmentAdmin role** — the caller needs `environments/editEnvironment` on the referenced environment

Enforced on both `create` and `modify` endpoints in `CredentialV1Controller`:
```java
@CheckPermissionByRequestProperty(
    path = "openstack.remoteEnvironmentCrn",
    type = CRN,
    action = EDIT_ENVIRONMENT,
    skipOnNull = true)
```

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| Registration in `authenticate()`, not `verify()` | Token fetch needs proxy route already available; `authenticate()` is called before `verify()` in the credential verification flow |
| `isRegistered` check before registering | Prevents duplicate registration on subsequent authenticate calls (idempotency) |
| CRN derived from accountId + credentialName | Deterministic — the URL resolver can reconstruct it without DB lookup |
| Service name normalization via `ServiceType.forName()` | Maps catalog names (e.g., `cinderv2`) to canonical names (`cinder`) that match openstack4j runtime lookups |
| Facing-based endpoint selection | The credential's `facing` field (internal/public) selects which interface from the catalog to register |
| Safe deregister on auth failure | Uses try/catch wrapper so cleanup failure doesn't mask the original authentication error |
| Deregistration failure doesn't block credential delete | Credential state is authoritative; orphaned proxy registrations are harmless and time out |

## Trigger Condition

The entire jumpgate flow is only active when `remoteEnvironmentCrn` is set on the
OpenStack credential. Without it, all OpenStack operations connect directly — no proxy,
no jumpgate, no behavioral change.
