## Core concept

The basic idea is that we have several secrets and we need to replace them with new value at several places.
Based on that, we would need a list of secrets and we would need to define all places, where the given secret should be replaced.
Rotation framework has been created based on this idea.

## Technical details

- `SecretType`: Every service needs to define the secrets that it handled via implementing this interface with an enum. The `List<SecretRotationStep> getSteps()` method returns in which order and where the secret needs to be updated.
  - additional flags can be defined for secret types:
    - internal: it can be rotated only via internal API call
    - skipSaltUpdate: if true, there is no need to update salt state before rotation
- `SecretRotationStep`: It defines the different steps where secrets can be updated. For example vault or user data.
- `AbstractRotationExecutor`: Implementations of this abstract class does the core work like rotating, rolling back, finalizing the secret rotation. The payload should extend the `RotationContext` class.
  - rotation: executes the actual replacement/rotation
  - rollback: rolling back the rotation if there is any error
  - finalize: cleans up the leftover after the successful execution of rotation
- `RotationContext`: Used to hold relevant data for the rotation of the given `SecretRotationStep`
- `RotationContextProvider`: This class assembles the payloads for the various rotation steps.
- `SecretTypeFlag`: This enum class contains specific flags/options for secret types:
  - `INTERNAL`: the given secret type can be used internally, which usually means the rotation endpoint can be called with internal actor only for that secret type.
  - `POST_FLOW`: the given secret type has a corresponding, specific flow, which needed to be executed after the rotation flow. The flow should be defined in the corresponding `SecretRotationFlowEventProvider` implementation.
  - `SKIP_SALT_UPDATE`: every rotation flowchain begins with a salt update flow to update salt states if needed. If there is no salt related action for the given secret type, you can use this, to skip the salt update flow.
  - `SKIP_SALT_HIGHSTATE`: you have also option to skip salt highstate part of the salt update flow, if you need to update salt states, but you do not want to execute salt highstate for the entire cluster/resource.
  - `SKIP_STATUS_CHECK`: normally, we allow rotation for working clusters, but this option allows us to execute the rotation for resources with other statuses too, if needed and possible.

## Secret rotation flow
- during secret rotation we are creating a flowchain and every secret will have an own flow in the flowchain
  - first flow step executes the rotation itself. It basically goes through every step of the given secret and executes the corresponding `AbstractRotationExecutor` rotate implementation.
  - if it fails, it goes to rollback state, then default failure state. The payload of the flow step should have the exception and the step, which failed. Based on that we will execute the `AbstractRotationExecutor` rollback implementation in reversed order starting from the failed step.
  - if it succeeds, it goes to finalize state, then final state of the flow. It basically goes through every step of the given secret and executes the corresponding `AbstractRotationExecutor` finalize implementation.
- every flow step constructs a map of rotation contexts and every step should have a corresponding context in the map
  - for this purpose, there is a `RotationContextProvider` and every secret should have a corresponding context provider, where we are constructing the map for the rotation flow 
- in the first and the last flow of the flowchain we are executing status updates
  - if a rotation flow goes to failure state, failure step also executes an additional status update 

## API
- for internal usage we have API, where we can define only one secret, and we can define the execution type of the rotation flow, we can tell the framework to execute only specific phase of the rotation (only rotation, only rollback, etc.). With that we can execute specific phase in a service initiated by another service, and later we can poll the status of the flow.
- for public usage we have API, where we can define the list of secrets in the request and here we can also define the execution type, but in case of public API we should use this only with caution and only in case of emergency (support cases, etc.)

## Guidelines
- where it is possible, we should create an additional user during rotation
  - in rotation step we should create new secret (and new corresponding user if needed) and use that for the corresponding component
  - in rollback phase we should remove the new secret (and corresponding user) and use the old one again
  - in finalize phase we should remove the old secret (with corresponding user)
- the flow engine ensures that we can continue the execution of rotation even if the service has been restarted in the meantime, though we need to ensure that every rotation step execution in every phase (rotate, rollback, finalize) is idempotent and multiple execution of them will give the same result

## Vault
- Vault has a central role in secret rotation, since most of these sensitive values are stored in Vault.
- Please check README under secret-engine module for further details about how to rotate sensitive data in Vault.

## Example
- Rotation: CM admin user of a datahub cluster
  - CloudbreakSecretType: CLOUDBREAK_CM_ADMIN_PASSWORD value in the enum
    - steps should be: 
      - VAULT: we should definitely update vault
      - CM_USER: we should update user in CM user database (with API)
      - CLUSTER_PROXY: for communication, we should update CP with the vault reference to the new secret
  - we should have an implemention of `RotationContextProvider`, let's say CBCMUAdminserRotationContextProvider, where we construct a 3 size map of rotation contexts:
    - VaultRotationContext: contains data about the path of affected secrets and about the new values of the given secrets
    - CMUserRotationContext: contains data about the path of affected secrets and about path of user/pass secrets which will be used for the API client
    - ClusterProxyReRegisterRotationContext: contains no more extra data
  - every rotation steps will have an `AbstractRotationExecutor`
    - VaultRotationExecutor:
      - rotate: adds new value to the existing path and store the old one in a different field of the path
      - rollback: backup value should be written back to the original field
      - finalize: backup field should be removed
    - CMUserRotationExecutor:
      - rotate: creates new user in CM using API client
      - rollback: removes new user in CM using API client if exists
      - finalize: removes old user in CM using API client
    - ClusterProxyRotationExecutor:
      - rotate: reregister stack in CP (using existing logic)
      - rollback: reregister stack in CP (using existing logic and using backup fields from Vault for the affected secrets)
      - finalize: nothing to do