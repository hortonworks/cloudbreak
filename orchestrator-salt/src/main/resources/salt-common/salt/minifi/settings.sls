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
}) %}