
# ConfigFile

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | [**TypeEnum**](#TypeEnum) | Config file in the standard format like xml, properties, json, yaml, template. |  [optional]
**destFile** | **String** | The path that this configuration file should be created as. If it is an absolute path, it will be mounted into the DOCKER container. Absolute paths are only allowed for DOCKER containers.  If it is a relative path, only the file name should be provided, and the file will be created in the container local working directory under a folder named conf. |  [optional]
**srcFile** | **String** | This provides the source location of the configuration file, the content of which is dumped to dest_file post property substitutions, in the format as specified in type. Typically the src_file would point to a source controlled network accessible file maintained by tools like puppet, chef, or hdfs etc. Currently, only hdfs is supported. |  [optional]
**properties** | **Map&lt;String, String&gt;** | A blob of key value pairs that will be dumped in the dest_file in the format as specified in type. If src_file is specified, src_file content are dumped in the dest_file and these properties will overwrite, if any, existing properties in src_file or be added as new properties in src_file. |  [optional]


<a name="TypeEnum"></a>
## Enum: TypeEnum
Name | Value
---- | -----
XML | &quot;XML&quot;
PROPERTIES | &quot;PROPERTIES&quot;
JSON | &quot;JSON&quot;
YAML | &quot;YAML&quot;
TEMPLATE | &quot;TEMPLATE&quot;
HADOOP_XML | &quot;HADOOP_XML&quot;
STATIC | &quot;STATIC&quot;
ARCHIVE | &quot;ARCHIVE&quot;



