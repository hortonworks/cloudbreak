
# Container

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** | Unique container id of a running service, e.g. container_e3751_1458061340047_0008_01_000002. |  [optional]
**launchTime** | [**LocalDate**](LocalDate.md) | The time when the container was created, e.g. 2016-03-16T01:01:49.000Z. This will most likely be different from cluster launch time. |  [optional]
**ip** | **String** | IP address of a running container, e.g. 172.31.42.141. The IP address and hostname attribute values are dependent on the cluster/docker network setup as per YARN-4007. |  [optional]
**hostname** | **String** | Fully qualified hostname of a running container, e.g. ctr-e3751-1458061340047-0008-01-000002.examplestg.site. The IP address and hostname attribute values are dependent on the cluster/docker network setup as per YARN-4007. |  [optional]
**bareHost** | **String** | The bare node or host in which the container is running, e.g. cn008.example.com. |  [optional]
**state** | [**ContainerState**](ContainerState.md) | State of the container of a service. |  [optional]
**componentInstanceName** | **String** | Name of the component instance that this container instance belongs to. Component instance name is named as $COMPONENT_NAME-i, where i is a monotonically increasing integer. E.g. A componet called nginx can have multiple component instances named as nginx-0, nginx-1 etc. Each component instance is backed by a container instance. |  [optional]
**resource** | [**Resource**](Resource.md) | Resource used for this container. |  [optional]
**artifact** | [**Artifact**](Artifact.md) | Artifact used for this container. |  [optional]
**privilegedContainer** | **Boolean** | Container running in privileged mode or not. |  [optional]



