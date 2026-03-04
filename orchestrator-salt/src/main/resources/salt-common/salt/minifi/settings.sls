{% set minifi = {} %}
{% if salt['pillar.get']('fluent:enabled') %}
    {% set minifi_enabled = True %}
{% else %}
    {% set minifi_enabled = False %}
{% endif %}

{% set minifi_rpm = 'https://cloudera-build-us-west-1.vpc.cloudera.com/s3/build/71542311/cem-agents/1.x/ubuntu22/apt/tars/nifi-minifi-cpp/nifi-minifi-cpp-1.25.09-b38-x86_64.rpm' %}
{% if salt['pillar.get']('fluent:cloudStorageLoggingEnabled') %}
    {% set cloud_storage_logging_enabled = True %}
{% else %}
    {% set cloud_storage_logging_enabled = False %}
{% endif %}

{% if salt['pillar.get']('fluent:region') %}
  {%- set region = salt['pillar.get']('fluent:region') %}
{% else %}
  {%- set region = None %}
{% endif %}
{% if grains['init'] == 'upstart' %}
    {% set is_systemd = False %}
{% else %}
    {% set is_systemd = True %}
{% endif %}
{% set provider_prefix = salt['pillar.get']('fluent:providerPrefix') %}
{% set log_folder = salt['pillar.get']('fluent:logFolderName') %}
{% set s3_log_bucket = salt['pillar.get']('fluent:s3LogArchiveBucketName') %}
{% set azure_container = salt['pillar.get']('fluent:azureContainer') %}
{% set azore_storage_account = salt['pillar.get']('fluent:azureStorageAccount') %}
{% set gcs_bucket = salt['pillar.get']('fluent:gcsBucket') %}

{% if salt['pillar.get']('fluent:dbusIncludeSaltLogs') %}
    {% set dbus_include_salt_logs = True %}
{% else %}
    {% set dbus_include_salt_logs = False %}
{% endif %}

{% set partition_interval = salt['pillar.get']('fluent:partitionIntervalMin') %}
{% set minifi_installed = salt['file.directory_exists' ]('/etc/nifi-minifi-cpp') %}

{%- set base_extensions = '/usr/lib64/nifi-minifi-cpp/extensions/libminifi-archive-extensions.so,/usr/lib64/nifi-minifi-cpp/extensions/libminifi-expression-language-extensions.so,/usr/lib64/nifi-minifi-cpp/extensions/libminifi-rocksdb-repos.so,/usr/lib64/nifi-minifi-cpp/extensions/libminifi-standard-processors.so,/usr/lib64/nifi-minifi-cpp/extensions/libminifi-systemd.so,/usr/lib64/nifi-minifi-cpp/extensions/minifi_native.so' %}

{%- if provider_prefix == "s3" %}
    {% set nifi_extension_path = base_extensions ~ ',/usr/lib64/nifi-minifi-cpp/extensions/libminifi-aws.so' %}
{%- elif provider_prefix == "abfs" %}
    {% set nifi_extension_path = base_extensions ~ ',/usr/lib64/nifi-minifi-cpp/extensions/libminifi-azure.so' %}
{%- elif provider_prefix == "gcs" %}
    {% set nifi_extension_path = base_extensions ~ ',/usr/lib64/nifi-minifi-cpp/extensions/libminifi-gcp.so' %}
{%- else %}
    {% set nifi_extension_path = base_extensions %}
{%- endif %}

{% set minifi_properties = {
    'nifi.flowfile.repository.rocksdb.compression': 'auto',
    'nifi.extension.path': nifi_extension_path,
    'nifi.flow.engine.threads': '5',
    'nifi.content.repository.class.name': 'FileSystemRepository',
    'nifi.flowfile.repository.rocksdb.write.buffer.size': '2 MB',
    'nifi.flowfile.repository.rocksdb.max.write.buffer.number': '2',
    'nifi.flowfile.repository.rocksdb.options': 'table_factory={block_cache=2M;};memtable_factory=SkipListFactory;',
    'nifi.flowfile.repository.rocksdb.compaction.period': '2 min',
    'nifi.database.content.repository.rocksdb.compaction.period': '2 min'
} %}

{% do minifi.update({
    "enabled": minifi_enabled,
    "is_systemd" : is_systemd,
    "providerPrefix": provider_prefix,
    "partitionIntervalMin": partition_interval,
    "logFolderName": log_folder,
    "cloudStorageLoggingEnabled": cloud_storage_logging_enabled,
    "s3LogArchiveBucketName" : s3_log_bucket,
    "azureStorageAccount": azore_storage_account,
    "azureContainer": azure_container,
    "gcsBucket": gcs_bucket,
    "dbusIncludeSaltLogs": dbus_include_salt_logs,
    "region": region,
    "minifiRpm": minifi_rpm,
    "minifiInstalled": minifi_installed,
    "minifiProperties": minifi_properties,
}) %}