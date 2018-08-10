
# ReadinessCheck

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | [**TypeEnum**](#TypeEnum) | DEFAULT (AM checks whether the container has an IP and optionally performs a DNS lookup for the container hostname), HTTP (AM performs default checks, plus sends a REST call to the container and expects a response code between 200 and 299), or PORT (AM performs default checks, plus attempts to open a socket connection to the container on a specified port). | 
**properties** | **Map&lt;String, String&gt;** | A blob of key value pairs that will be used to configure the check. |  [optional]
**artifact** | [**Artifact**](Artifact.md) | Artifact of the pluggable readiness check helper container (optional). If specified, this helper container typically hosts the http uri and encapsulates the complex scripts required to perform actual container readiness check. At the end it is expected to respond a 204 No content just like the simplified use case. This pluggable framework benefits service owners who can run services without any packaging modifications. Note, artifacts of type docker only is supported for now. NOT IMPLEMENTED YET |  [optional]


<a name="TypeEnum"></a>
## Enum: TypeEnum
Name | Value
---- | -----
DEFAULT | &quot;DEFAULT&quot;
HTTP | &quot;HTTP&quot;
PORT | &quot;PORT&quot;



