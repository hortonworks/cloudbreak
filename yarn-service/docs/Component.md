
# Component

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **String** | Name of the service component (mandatory). If Registry DNS is enabled, the max length is 63 characters. If unique component support is enabled, the max length is lowered to 44 characters. | 
**state** | [**ComponentState**](ComponentState.md) | The state of the component |  [optional]
**dependencies** | **List&lt;String&gt;** | An array of service components which should be in READY state (as defined by readiness check), before this component can be started. The dependencies across all components of a service should be represented as a DAG. |  [optional]
**readinessCheck** | [**ReadinessCheck**](ReadinessCheck.md) | Readiness check for this component. |  [optional]
**artifact** | [**Artifact**](Artifact.md) | Artifact of the component (optional). If not specified, the service level global artifact takes effect. |  [optional]
**launchCommand** | **String** | The custom launch command of this component (optional for DOCKER component, required otherwise). When specified at the component level, it overrides the value specified at the global level (if any). |  [optional]
**resource** | [**Resource**](Resource.md) | Resource of this component (optional). If not specified, the service level global resource takes effect. |  [optional]
**numberOfContainers** | **Long** | Number of containers for this component (optional). If not specified, the service level global number_of_containers takes effect. |  [optional]
**containers** | [**List&lt;Container&gt;**](Container.md) | Containers of a started component. Specifying a value for this attribute for the POST payload raises a validation error. This blob is available only in the GET response of a started service. |  [optional]
**runPrivilegedContainer** | **Boolean** | Run all containers of this component in privileged mode (YARN-4262). |  [optional]
**placementPolicy** | [**PlacementPolicy**](PlacementPolicy.md) | Advanced scheduling and placement policies for all containers of this component. |  [optional]
**_configuration** | [**ModelConfiguration**](ModelConfiguration.md) | Config properties for this component. |  [optional]
**quicklinks** | **List&lt;String&gt;** | A list of quicklink keys defined at the service level, and to be resolved by this component. |  [optional]
**restartPolicy** | [**RestartPolicyEnum**](#RestartPolicyEnum) | Policy of restart component. Including ALWAYS (Always restart component even if instance exit code &#x3D; 0); ON_FAILURE (Only restart component if instance exit code !&#x3D; 0); NEVER (Do not restart in any cases) |  [optional]


<a name="RestartPolicyEnum"></a>
## Enum: RestartPolicyEnum
Name | Value
---- | -----
ALWAYS | &quot;ALWAYS&quot;
ON_FAILURE | &quot;ON_FAILURE&quot;
NEVER | &quot;NEVER&quot;



