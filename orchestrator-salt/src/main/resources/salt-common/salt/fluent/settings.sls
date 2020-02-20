{% set fluent = {} %}
{% if salt['pillar.get']('fluent:enabled') %}
    {% set fluent_enabled = True %}
{% else %}
    {% set fluent_enabled = False %}
{% endif %}
{% if salt['pillar.get']('fluent:cloudStorageLoggingEnabled') %}
    {% set cloud_storage_logging_enabled = True %}
{% else %}
    {% set cloud_storage_logging_enabled = False %}
{% endif %}
{% if salt['pillar.get']('fluent:cloudLoggingServiceEnabled') %}
    {% set cloud_logging_service_enabled = True %}
    {% if salt['pillar.get']('fluent:platform') == "AWS" %}
      {%- set instanceDetails = salt.cmd.run('curl -s http://169.254.169.254/latest/dynamic/instance-identity/document') | load_json %}
      {%- set region = instanceDetails['region'] %}
    {% else %}
      {%- set region = salt['pillar.get']('fluent:region') %}
    {% endif %}
{% else %}
    {% set cloud_logging_service_enabled = False %}
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
{% set cloudwatch_stream_key = salt['pillar.get']('fluent:cloudwatchStreamKey') %}
{% set s3_log_bucket = salt['pillar.get']('fluent:s3LogArchiveBucketName') %}
{% set azure_container = salt['pillar.get']('fluent:azureContainer') %}
{% set azure_storage_instance_msi = salt['pillar.get']('fluent:azureInstanceMsi') %}
{% set azore_storage_account = salt['pillar.get']('fluent:azureStorageAccount') %}
{% set azure_storage_access_key = salt['pillar.get']('fluent:azureStorageAccessKey') %}

{% if salt['pillar.get']('fluent:dbusClusterLogsCollection') %}
    {% set dbus_cluster_logs_collection_enabled = True %}
{% else %}
    {% set dbus_cluster_logs_collection_enabled = False %}
{% endif %}

{% if salt['pillar.get']('fluent:dbusClusterLogsCollectionDisableStop') %}
    {% set dbus_cluster_logs_collection_disable_stop = True %}
{% else %}
    {% set dbus_cluster_logs_collection_disable_stop = False %}
{% endif %}

{% if salt['pillar.get']('fluent:dbusMeteringEnabled') %}
    {% set dbus_metering_enabled = True %}
{% else %}
    {% set dbus_metering_enabled = False %}
{% endif %}

{% set cluster_name = salt['pillar.get']('fluent:clusterName') %}
{% set cluster_type = salt['pillar.get']('fluent:clusterType')%}
{% set cluster_crn = salt['pillar.get']('fluent:clusterCrn')%}
{% set cluster_owner = salt['pillar.get']('fluent:clusterOwner')%}
{% set cluster_version = salt['pillar.get']('fluent:clusterVersion')%}

{% set partition_interval = salt['pillar.get']('fluent:partitionIntervalMin') %}
{% set cloudera_public_gem_repo = 'https://repository.cloudera.com/cloudera/api/gems/cloudera-gems/' %}
{% set cloudera_azure_plugin_version = '1.0.1' %}
{% set cloudera_azure_gen2_plugin_version = '0.2.4' %}
{% set cloudera_databus_plugin_version = '1.0.3' %}
{% set redaction_plugin_version = '0.1.2' %}
{% set platform = salt['pillar.get']('fluent:platform') %}

{% set service_path_log_suffix = '%Y-%m-%d/%H/\${tag[1]}-#{Socket.gethostname}-%M' %}
{% set cm_command_path_log_suffix = '%Y-%m-%d/%H/CM_COMMAND-\${tag[6]}-\${tag[1]}-#{Socket.gethostname}-%M' %}

{% set number_of_workers=0 %}
{% set cloud_storage_worker_index=0 %}
{% set metering_worker_index=0 %}
{% set cluster_logs_collection_worker_index=0 %}
{% if cloud_storage_logging_enabled or cloud_logging_service_enabled %}
{%   set cloud_storage_worker_index=number_of_workers %}
{%   set number_of_workers=number_of_workers+1 %}
{% endif %}
{% if dbus_metering_enabled %}
{%   set metering_worker_index=number_of_workers %}
{%   set number_of_workers=number_of_workers+1 %}
{% endif %}
{% if dbus_cluster_logs_collection_enabled %}
{%   set cluster_logs_collection_worker_index=number_of_workers %}
{%   set number_of_workers=number_of_workers+1 %}
{% endif %}

{% set anonymization_rules=[] %}
{% if salt['pillar.get']('fluent:anonymizationRules') %}
{%   set anonymization_rules = salt['pillar.get']('fluent:anonymizationRules') %}
{% endif %}

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
    "clusterName": cluster_name,
    "clusterType": cluster_type,
    "clusterCrn": cluster_crn,
    "clusterOwner": cluster_owner,
    "clusterVersion": cluster_version,
    "cloudStorageLoggingEnabled": cloud_storage_logging_enabled,
    "cloudLoggingServiceEnabled": cloud_logging_service_enabled,
    "cloudwatchStreamKey": cloudwatch_stream_key,
    "s3LogArchiveBucketName" : s3_log_bucket,
    "azureStorageAccount": azore_storage_account,
    "azureContainer": azure_container,
    "azureInstanceMsi": azure_storage_instance_msi,
    "azureStorageAccessKey": azure_storage_access_key,
    "dbusClusterLogsCollection": dbus_cluster_logs_collection_enabled,
    "dbusClusterLogsCollectionDisableStop": dbus_cluster_logs_collection_disable_stop,
    "dbusMeteringEnabled": dbus_metering_enabled,
    "clouderaPublicGemRepo": cloudera_public_gem_repo,
    "clouderaAzurePluginVersion": cloudera_azure_plugin_version,
    "clouderaAzureGen2PluginVersion": cloudera_azure_gen2_plugin_version,
    "clouderaDatabusPluginVersion": cloudera_databus_plugin_version,
    "redactionPluginVersion": redaction_plugin_version,
    "numberOfWorkers": number_of_workers,
    "cloudStorageWorkerIndex": cloud_storage_worker_index,
    "meteringWorkerIndex": metering_worker_index,
    "clusterLogsCollectionWorkerIndex": cluster_logs_collection_worker_index,
    "anonymizationRules": anonymization_rules,
    "region": region,
    "platform": platform
}) %}