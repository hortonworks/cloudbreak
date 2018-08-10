
# KerberosPrincipal

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**principalName** | **String** | The principal name of the user who launches the service. Note that &#x60;_HOST&#x60; is required in the &#x60;principal_name&#x60; field such as &#x60;testuser/_HOST@EXAMPLE.COM&#x60; because Hadoop client validates that the server&#39;s (in this case, the AM&#39;s) principal has hostname present when communicating to the server. |  [optional]
**keytab** | **String** | The URI of the kerberos keytab. Currently supports only files present on the bare host. URI starts with \&quot;file\\://\&quot; - A path on the local host where the keytab is stored. It is assumed that admin pre-installs the keytabs on the local host before AM launches. |  [optional]



