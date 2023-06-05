## Core concept

The basic idea is that we have several secrets and we need to replace them with new value at several places.
Based on that, we would need a list of secrets and we would need to define all places, where the given secret should be replaced.
Rotation framework has been created based on this idea.

## Technical details

- `SecretType`: Every service needs to define the secrets that it handled via implementing this interface with an enum. The `List<SecretRotationStep> getSteps()` method returns in which order and where the secret needs to be updated.
- `SecretRotationStep`: It defines the different steps where secrets can be updated. For example vault or user data.
- `RotationExecutor`: Implementations of this interface does the core work like rotating, rolling back, finalizing the secret rotation. The payload should extend the `RotationContext` class.
  - rotation: executes the actual replacement/rotation
  - rollback: rolling back the rotation if there is any error
  - finalize: cleans up the leftover after the successful execution of rotation
- `RotationContext`: Used to hold relevant data for the rotation of the given `SecretRotationStep`
- `RotationContextProvider`: This class assembles the payloads for the various rotation steps.
- `ApplicationSecretRotationInformation`: Each service defines the secret type that it handles via this interface.

## Secret rotation flow
- during secret rotation we are creating a flowchain and every secret will have an own flow in the flowchain
  - first flow step executes the rotation itself. It basically goes through every step of the given secret and executes the corresponding `RotationExecutor` rotate implementation.
  - if it fails, it goes to rollback state, then default failure state. The payload of the flow step should have the exception and the step, which failed. Based on that we will execute the `RotationExecutor` rollback implementation in reversed order starting from the failed step.
  - if it succeeds, it goes to finalize state, then final state of the flow. It basically goes through every step of the given secret and executes the corresponding `RotationExecutor` finalize implementation.
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

## Example
- CM admin user of a datahub cluster
  - CloudbreakSecretType: CLOUDBREAK_CM_ADMIN_PASSWORD value in the enum
    - steps should be: 
      - VAULT: we should definitely update vault
      - CM_USER: we should update user in CM user database (with API)
      - CLUSTER_PROXY: for communication, we should update CP with the vault reference to the new secret
  - we should have an implemention of `RotationContextProvider`, let's say CBCMUAdminserRotationContextProvider, where we construct a 3 size map of rotation contexts:
    - VaultRotationContext: contains data about the path of affected secrets and about the new values of the given secrets
    - CMUserRotationContext: contains data about the path of affected secrets and about path of user/pass secrets which will be used for the API client
    - ClusterProxyRotationContext: contains no more extra data
  - every rotation steps will have an `RotationExecutor`
    - VaultRotationExecutor:
      - rotate: adds new value to the existing path and store the old one in a different field of the path
      - rollback: backup value should be written back to the original field
      - finalize: backup field should be removed
    - CMUserRotationExecutor:
      - rotate: creates new user in CM using API client
      - rollback: removes new user in CM using API client if exists
      - finalize: removes old user in CM using API client
    - ClusterproxyRotationExecutor:
      - rotate: reregister stack in CP (using existing logic)
      - rollback: reregister stack in CP (using existing logic and using backup fields from Vault for the affected secrets)
      - finalize: nothing to do