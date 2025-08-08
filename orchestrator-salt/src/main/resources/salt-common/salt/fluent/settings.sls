{% set fluent = {} %}
{% if salt['pillar.get']('fluent:enabled') %}
    {% set fluent_enabled = True %}
{% else %}
    {% set fluent_enabled = False %}
{% endif %}
{% set os = salt['grains.get']('os') %}
{% set cpuarch = salt['grains.get']('cpuarch') %}
{% set cdp_logging_agent_version = '1.3.5' %}
{% set cdp_logging_agent_build_number = 'b1' %}
{% set cdp_logging_agent_dev_version = '0.1.0*' %}
{% if os == "RedHat" and grains['osmajorrelease'] | int == 8 %}
    {% if cpuarch != 'aarch64' %}
        {% set rpm_os = 'redhat8' %}
    {% else %}
        {% set rpm_os = 'redhat8arm64' %}
    {% endif %}
{% elif os == "RedHat" and grains['osmajorrelease'] | int == 9 %}
    {% if cpuarch != 'aarch64' %}
        {% set rpm_os = 'redhat9' %}
    {% else %}
        {% set rpm_os = 'redhat9arm64' %}
    {% endif %}
{% elif os == "CentOS" %}
    {% set rpm_os = 'redhat7' %}
{% endif %}
{% set cdp_logging_agent_rpm = 'https://archive.cloudera.com/cdp-infra-tools/' + cdp_logging_agent_version + '/' + rpm_os + '/yum/cdp_logging_agent-' + cdp_logging_agent_version + '_' +  cdp_logging_agent_build_number + '.rpm' %}
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
{% if salt['pillar.get']('fluent:azureIdBrokerInstanceMsi') %}
    {% set azure_storage_idbroker_instance_msi = salt['pillar.get']('fluent:azureIdBrokerInstanceMsi') %}
{% else %}
    {% set azure_storage_idbroker_instance_msi = salt['pillar.get']('fluent:azureInstanceMsi') %}
{% endif %}
{% set azore_storage_account = salt['pillar.get']('fluent:azureStorageAccount') %}
{% set azure_storage_access_key = salt['pillar.get']('fluent:azureStorageAccessKey') %}
{% set gcs_bucket = salt['pillar.get']('fluent:gcsBucket') %}
{% set gcs_project_id = salt['pillar.get']('fluent:gcsProjectId') %}

{% if salt['pillar.get']('fluent:dbusIncludeSaltLogs') %}
    {% set dbus_include_salt_logs = True %}
{% else %}
    {% set dbus_include_salt_logs = False %}
{% endif %}

{% set cluster_name = salt['pillar.get']('fluent:clusterName') %}
{% set cluster_type = salt['pillar.get']('fluent:clusterType')%}
{% set cluster_crn = salt['pillar.get']('fluent:clusterCrn')%}
{% set cluster_owner = salt['pillar.get']('fluent:clusterOwner')%}
{% set cluster_version = salt['pillar.get']('fluent:clusterVersion')%}
{% set environment_region = salt['pillar.get']('fluent:environmentRegion')%}

{% set partition_interval = salt['pillar.get']('fluent:partitionIntervalMin') %}
{% set cloudera_public_gem_repo = 'https://repository.cloudera.com/cloudera/api/gems/cloudera-gems/' %}
{% set cloudera_azure_plugin_version = '1.0.1' %}
{% set cloudera_azure_gen2_plugin_version = '0.3.1' %}
{% set cloudera_databus_plugin_version = '1.0.5' %}
{% set redaction_plugin_version = '0.1.2' %}
{% set platform = salt['pillar.get']('fluent:platform') %}

{% set service_path_log_suffix = '%Y-%m-%d/%H/\${tag[1]}-#{Socket.gethostname}-%M' %}
{% set cm_command_path_log_suffix = '%Y-%m-%d/%H/CM_COMMAND-\${tag[6]}-\${tag[1]}-#{Socket.gethostname}-%M' %}

{% set number_of_workers=0 %}
{% set cloud_storage_worker_index=0 %}
{% if cloud_storage_logging_enabled %}
{%   set cloud_storage_worker_index=number_of_workers %}
{%   set number_of_workers=number_of_workers+1 %}
{% endif %}

{% set anonymization_rules=[] %}
{% if salt['pillar.get']('fluent:anonymizationRules') %}
{%   set anonymization_rules = salt['pillar.get']('fluent:anonymizationRules') %}
{% endif %}

{% if salt['pillar.get']('proxy:host') %}
  {% set proxy_host = salt['pillar.get']('proxy:host') %}
  {% set proxy_port = salt['pillar.get']('proxy:port')|string %}
  {% set proxy_protocol = salt['pillar.get']('proxy:protocol') %}
  {% set proxy_url = proxy_protocol + "://" + proxy_host + ":" + proxy_port %}
  {% if salt['pillar.get']('proxy:user') and salt['pillar.get']('proxy:password') %}
    {% set proxy_auth = True %}
    {% set proxy_user = salt['pillar.get']('proxy:user') %}
    {% set proxy_password = salt['pillar.get']('proxy:password') %}
    {% set proxy_full_url =  proxy_protocol + "://" + proxy_user + ":"+ proxy_password + "@" + proxy_host + ":" + proxy_port %}
  {% else %}
    {% set proxy_auth = False %}
    {% set proxy_full_url = proxy_url %}
  {% endif %}
{% else %}
  {% set proxy_url = None %}
  {% set proxy_user = None %}
  {% set proxy_password = None %}
  {% set proxy_auth = False %}
  {% set proxy_full_url = None %}
{% endif %}
{% set no_proxy_hosts = salt['pillar.get']('proxy:noProxyHosts') %}
{% set forward_port = 24224 %}
{% set version_data = namespace(entities=[]) %}
{% for role in grains.get('roles', []) %}
{% if role.startswith("fluent_prewarmed") %}
  {% set version_data.entities = version_data.entities + [role.split("fluent_prewarmed_v")[1]]%}
{% endif %}
{% endfor %}
{% if version_data.entities|length > 0 %}
{% set fluent_version = version_data.entities[0] | int %}
{% else %}
{% set fluent_version = 0 %}
{% endif %}
{% set td_agent_installed = salt['file.directory_exists' ]('/etc/td-agent') %}
{% set cdp_logging_agent_installed = salt['file.directory_exists' ]('/etc/cdp-logging-agent') %}
{% if td_agent_installed and cdp_logging_agent_installed %}
  {% set binary = 'cdp-logging-agent' %}
  {% set uninstall_td_agent = True %}
{% elif td_agent_installed %}
  {% set binary = 'td-agent' %}
  {% set uninstall_td_agent = False %}
{% else %}
  {% set binary = 'cdp-logging-agent' %}
  {% set uninstall_td_agent = False %}
{% endif %}
{% set cdp_logging_agent_package_version = salt['pkg.version']('cdp-logging-agent') %}
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
    "s3LogArchiveBucketName" : s3_log_bucket,
    "azureStorageAccount": azore_storage_account,
    "azureContainer": azure_container,
    "azureInstanceMsi": azure_storage_instance_msi,
    "azureIdBrokerInstanceMsi": azure_storage_idbroker_instance_msi,
    "azureStorageAccessKey": azure_storage_access_key,
    "gcsBucket": gcs_bucket,
    "gcsProjectId": gcs_project_id,
    "clouderaPublicGemRepo": cloudera_public_gem_repo,
    "clouderaAzurePluginVersion": cloudera_azure_plugin_version,
    "clouderaAzureGen2PluginVersion": cloudera_azure_gen2_plugin_version,
    "clouderaDatabusPluginVersion": cloudera_databus_plugin_version,
    "redactionPluginVersion": redaction_plugin_version,
    "numberOfWorkers": number_of_workers,
    "cloudStorageWorkerIndex": cloud_storage_worker_index,
    "anonymizationRules": anonymization_rules,
    "dbusIncludeSaltLogs": dbus_include_salt_logs,
    "forwardPort" : 24224,
    "region": region,
    "environmentRegion" : environment_region,
    "platform": platform,
    "proxyUrl": proxy_url,
    "proxyAuth": proxy_auth,
    "proxyUser": proxy_user,
    "proxyPassword": proxy_password,
    "proxyFullUrl": proxy_full_url,
    "noProxyHosts": no_proxy_hosts,
    "binary": binary,
    "fluentVersion": fluent_version,
    "cdpLoggingAgentInstalled": cdp_logging_agent_installed,
    "cdpLoggingAgentRpm": cdp_logging_agent_rpm,
    "cdpLoggingAgentPackageVersion": cdp_logging_agent_package_version,
    "cdpLoggingAgentDevVersion": cdp_logging_agent_dev_version,
    "uninstallTdAgent": uninstall_td_agent
}) %}