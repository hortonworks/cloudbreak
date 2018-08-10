
# ServiceStatus

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**diagnostics** | **String** | Diagnostic information (if any) for the reason of the current state of the service. It typically has a non-null value, if the service is in a non-running state. |  [optional]
**state** | [**ServiceState**](ServiceState.md) | Service state. |  [optional]
**code** | **Integer** | An error code specific to a scenario which service owners should be able to use to understand the failure in addition to the diagnostic information. |  [optional]



