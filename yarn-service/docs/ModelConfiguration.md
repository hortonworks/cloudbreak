
# ModelConfiguration

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**properties** | **Map&lt;String, String&gt;** | A blob of key-value pairs for configuring the YARN service AM. |  [optional]
**env** | **Map&lt;String, String&gt;** | A blob of key-value pairs which will be appended to the default system properties and handed off to the service at start time. All placeholder references to properties will be substituted before injection. |  [optional]
**files** | [**List&lt;ConfigFile&gt;**](ConfigFile.md) | Array of list of files that needs to be created and made available as volumes in the service component containers. |  [optional]



