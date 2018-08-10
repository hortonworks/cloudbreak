
# Artifact

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** | Artifact id. Examples are package location uri for tarball based services, image name for docker, name of service, etc. | 
**type** | [**TypeEnum**](#TypeEnum) | Artifact type, like docker, tarball, etc. (optional). For TARBALL type, the specified tarball will be localized to the container local working directory under a folder named lib. For SERVICE type, the service specified will be read and its components will be added into this service. The original component with artifact type SERVICE will be removed (any properties specified in the original component will be ignored). |  [optional]
**uri** | **String** | Artifact location to support multiple artifact stores (optional). |  [optional]


<a name="TypeEnum"></a>
## Enum: TypeEnum
Name | Value
---- | -----
DOCKER | &quot;DOCKER&quot;
TARBALL | &quot;TARBALL&quot;
SERVICE | &quot;SERVICE&quot;



