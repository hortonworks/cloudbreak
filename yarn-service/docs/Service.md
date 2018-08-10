
# Service

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **String** | A unique service name. If Registry DNS is enabled, the max length is 63 characters. | 
**version** | **String** | Version of the service. | 
**description** | **String** | Description of the service. |  [optional]
**id** | **String** | A unique service id. |  [optional]
**artifact** | [**Artifact**](Artifact.md) | The default artifact for all components of the service except the components which has Artifact type set to SERVICE (optional). |  [optional]
**resource** | [**Resource**](Resource.md) | The default resource for all components of the service (optional). |  [optional]
**launchTime** | [**LocalDate**](LocalDate.md) | The time when the service was created, e.g. 2016-03-16T01:01:49.000Z. |  [optional]
**numberOfRunningContainers** | **Long** | In get response this provides the total number of running containers for this service (across all components) at the time of request. Note, a subsequent request can return a different number as and when more containers get allocated until it reaches the total number of containers or if a flex request has been made between the two requests. |  [optional]
**lifetime** | **Long** | Life time (in seconds) of the service from the time it reaches the STARTED state (after which it is automatically destroyed by YARN). For unlimited lifetime do not set a lifetime value. |  [optional]
**components** | [**List&lt;Component&gt;**](Component.md) | Components of a service. |  [optional]
**_configuration** | [**ModelConfiguration**](ModelConfiguration.md) | Config properties of a service. Configurations provided at the service/global level are available to all the components. Specific properties can be overridden at the component level. |  [optional]
**state** | [**ServiceState**](ServiceState.md) | State of the service. Specifying a value for this attribute for the PUT payload means update the service to this desired state. |  [optional]
**quicklinks** | **Map&lt;String, String&gt;** | A blob of key-value pairs of quicklinks to be exported for a service. |  [optional]
**queue** | **String** | The YARN queue that this service should be submitted to. |  [optional]
**kerberosPrincipal** | [**KerberosPrincipal**](KerberosPrincipal.md) | The principal info of the user who launches the service. |  [optional]
**dockerClientConfig** | **String** | URI of the file containing the docker client configuration (e.g. hdfs:///tmp/config.json). |  [optional]



