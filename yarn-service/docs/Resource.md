
# Resource

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**profile** | **String** | Each resource profile has a unique id which is associated with a cluster-level predefined memory, cpus, etc. |  [optional]
**cpus** | **Integer** | Amount of vcores allocated to each container (optional but overrides cpus in profile if specified). |  [optional]
**memory** | **String** | Amount of memory allocated to each container (optional but overrides memory in profile if specified). Currently accepts only an integer value and default unit is in MB. |  [optional]
**additional** | [**Map&lt;String, ResourceInformation&gt;**](ResourceInformation.md) | A map of resource type name to resource type information. Including value (integer), and unit (string). This will be used to specify resource other than cpu and memory. Please refer to example below. |  [optional]



