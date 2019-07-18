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
{% set s3_log_bucket = salt['pillar.get']('fluent:s3LogArchiveBucketName') %}
{% set s3_log_folder = salt['pillar.get']('fluent:s3LogFolderName') %}
{% set partition_interval = salt['pillar.get']('fluent:partitionIntervalMin') %}
{% set cloudera_public_gem_repo = 'https://repository.cloudera.com/cloudera/api/gems/cloudera-gems/' %}
{% set cloudera_azure_plugin_version = '1.0.0' %}
{% set cloudera_databus_plugin_version = '1.0.2' %}
{% set platform = salt['pillar.get']('fluent:platform') %}

{% do fluent.update({
    "is_systemd" : is_systemd,
    "serverLogFolderPrefix": server_log_folder_prefix,
    "agentLogFolderPrefix": agent_log_folder_prefix,
    "serviceLogFolderPrefix": service_log_folder_prefix,
    "enabled": fluent_enabled,
    "user": fluent_user,
    "group": fluent_group,
    "providerPrefix": provider_prefix,
    "partitionIntervalMin": partition_interval,
    "s3LogArchiveBucketName" : s3_log_bucket,
    "s3LogFolderName": s3_log_folder,
    "clouderaPublicGemRepo": cloudera_public_gem_repo,
    "clouderaAzurePluginVersion": cloudera_azure_plugin_version,
    "clouderaDatabusPluginVersion": cloudera_databus_plugin_version,
    "platform": platform
}) %}