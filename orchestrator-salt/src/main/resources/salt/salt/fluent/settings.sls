{% set fluent = {} %}
{% if salt['pillar.get']('fluent:enabled') %}
    {% set fluent_enabled = True %}
{% else %}
    {% set fluent_enabled = False %}
{% endif %}
{% if grains['init'] == 'upstart' %}
    {% set is_systemd = False %}
{% else %}
    {% set is_systemd = True %}
{% endif %}
{% set fluent_user = salt['pillar.get']('fluent:user') %}
{% set fluent_group = salt['pillar.get']('fluent:group') %}
{% set server_log_folder_prefix = salt['pillar.get']('fluent:serverLogFolderPrefix') %}
{% set agent_log_folder_prefix = salt['pillar.get']('fluent:agentLogFolderPrefix') %}
{% set service_log_folder_prefix = salt['pillar.get']('fluent:serviceLogFolderPrefix') %}
{% set provider_prefix = salt['pillar.get']('fluent:providerPrefix') %}
{% set log_folder = salt['pillar.get']('fluent:logFolderName') %}
{% set s3_log_bucket = salt['pillar.get']('fluent:s3LogArchiveBucketName') %}
{% set azure_container = salt['pillar.get']('fluent:azureContainer') %}
{% set azure_storage_instance_msi = salt['pillar.get']('fluent:azureInstanceMsi') %}
{% set azore_storage_account = salt['pillar.get']('fluent:azureStorageAccount') %}
{% set azure_storage_access_key = salt['pillar.get']('fluent:azureStorageAccessKey') %}

{% set dbus_endpoint = salt['pillar.get']('fluent:dbusEndpoint') %}
{% set dbus_acces_key_id = salt['pillar.get']('fluent:dbusAccesKeyId') %}
{% set dbus_access_key_secret = salt['pillar.get']('fluent:dbusAccessKeySecret') %}
{% set dbus_access_key_secret_algorithm = salt['pillar.get']('fluent:dbusAccessKeySecretAlgorithm') %}

{% if salt['pillar.get']('fluent:dbusReportBundleEnabled') %}
    {% set dbus_report_bundle_enabled = True %}
{% else %}
    {% set dbus_report_bundle_enabled = False %}
{% endif %}

{% if salt['pillar.get']('fluent:dbusReportBundleDisableStop') %}
    {% set dbus_report_bundle_disable_stop = True %}
{% else %}
    {% set dbus_report_bundle_disable_stop = False %}
{% endif %}

{% if salt['pillar.get']('fluent:dbusMeteringEnabled') %}
    {% set dbus_metering_enabled = True %}
{% else %}
    {% set dbus_metering_enabled = False %}
{% endif %}

{% set dbus_valid = dbus_endpoint and dbus_acces_key_id and dbus_access_key_secret %}

{% set partition_interval = salt['pillar.get']('fluent:partitionIntervalMin') %}
{% set cloudera_public_gem_repo = 'https://repository.cloudera.com/cloudera/api/gems/cloudera-gems/' %}
{% set cloudera_azure_plugin_version = '1.0.1' %}
{% set cloudera_databus_plugin_version = '1.0.2' %}
{% set platform = salt['pillar.get']('fluent:platform') %}

{% set service_path_log_suffix = '%Y-%m-%d/%H/\${tag[1]}-#{Socket.gethostname}-%M' %}
{% set cm_command_path_log_suffix = '%Y-%m-%d/%H/CM_COMMAND-\${tag[6]}-\${tag[1]}-#{Socket.gethostname}-%M' %}

{% do fluent.update({
    "enabled": fluent_enabled,
    "is_systemd" : is_systemd,
    "serverLogFolderPrefix": server_log_folder_prefix,
    "agentLogFolderPrefix": agent_log_folder_prefix,
    "serviceLogFolderPrefix": service_log_folder_prefix,
    "serviceLogPathSuffix": service_path_log_suffix,
    "cmCommandLogPathSuffix": cm_command_path_log_suffix,
    "user": fluent_user,
    "group": fluent_group,
    "providerPrefix": provider_prefix,
    "partitionIntervalMin": partition_interval,
    "logFolderName": log_folder,
    "s3LogArchiveBucketName" : s3_log_bucket,
    "azureStorageAccount": azore_storage_account,
    "azureContainer": azure_container,
    "azureInstanceMsi": azure_storage_instance_msi,
    "azureStorageAccessKey": azure_storage_access_key,
    "dbusEndpoint": dbus_endpoint,
    "dbusAccesKeyId": dbus_acces_key_id,
    "dbusAccessKeySecret": dbus_access_key_secret,
    "dbusAccessKeySecretAlgorithm": dbus_access_key_secret_algorithm,
    "dbusValid": dbus_valid,
    "dbusReportBundleEnabled": dbus_report_bundle_enabled,
    "dbusReportBundleDisableStop": dbus_report_bundle_disable_stop,
    "dbusMeteringEnabled": dbus_metering_enabled,
    "clouderaPublicGemRepo": cloudera_public_gem_repo,
    "clouderaAzurePluginVersion": cloudera_azure_plugin_version,
    "clouderaDatabusPluginVersion": cloudera_databus_plugin_version,
    "platform": platform
}) %}