{% set monitoring = {} %}

{% set node_exporter_exists = salt['file.directory_exists' ]('/opt/node_exporter') %}
{% set blackbox_exporter_exists = salt['file.directory_exists' ]('/opt/blackbox_exporter') %}
{% if salt['pillar.get']('monitoring:enabled') %}
    {% set monitoring_enabled = True %}
{% else %}
    {% set monitoring_enabled = False %}
{% endif %}

{% if grains['init'] == 'upstart' %}
    {% set is_systemd = False %}
{% else %}
    {% set is_systemd = True %}
{% endif %}

{% set type = salt['pillar.get']('monitoring:type') %}
{% set cm_username = salt['pillar.get']('monitoring:cmUsername') %}
{% set cm_password = salt['pillar.get']('monitoring:cmPassword') %}
{% set agent_user = salt['pillar.get']('monitoring:agentUser', 'vmagent') %}
{% set agent_port = salt['pillar.get']('monitoring:agentPort', 8429) %}
{% set agent_max_disk_usage = salt['pillar.get']('monitoring:agentMaxDiskUsage', '4GB') %}
{% set retention_min_time = salt['pillar.get']('monitoring:retentionMinTime', '5m') %}
{% set retention_max_time = salt['pillar.get']('monitoring:retentionMaxTime', '4h') %}
{% set min_backoff = salt['pillar.get']('monitoring:minBackoff', '1m') %}
{% set max_backoff = salt['pillar.get']('monitoring:maxBackoff', '15m') %}
{% set max_shards = salt['pillar.get']('monitoring:maxShards', '50') %}
{% set max_samples_per_send = salt['pillar.get']('monitoring:maxSamplesPerSend', '2000') %}
{% set capacity = salt['pillar.get']('monitoring:capacity', '10000') %}
{% set wal_truncate_frequency = salt['pillar.get']('monitoring:walTruncateFrequency', '2h') %}
{% set node_exporter_user = salt['pillar.get']('monitoring:nodeExporterUser', 'nodeuser') %}
{% set node_exporter_port = salt['pillar.get']('monitoring:nodeExporterPort', 9100) %}
{% set node_exporter_collectors = salt['pillar.get']('monitoring:nodeExporterCollectors', []) %}
{% set blackbox_exporter_user = salt['pillar.get']('monitoring:blackboxExporterUser', 'blackboxuser') %}
{% set blackbox_exporter_port = salt['pillar.get']('monitoring:blackboxExporterPort', 9115) %}
{% set blackbox_exporter_cloud_interval_seconds = salt['pillar.get']('monitoring:blackboxExporterCloudIntervalSeconds', 600) %}
{% set blackbox_exporter_cloudera_interval_seconds = salt['pillar.get']('monitoring:blackboxExporterClouderaIntervalSeconds', 1800) %}
{% set blackbox_exporter_check_on_all_nodes = salt['pillar.get']('monitoring:blackboxExporterCheckOnAllNodes', false) %}
{% set blackbox_exporter_cloud_links = salt['pillar.get']('monitoring:blackboxExporterCloudLinks', []) %}
{% set blackbox_exporter_cloudera_links = salt['pillar.get']('monitoring:blackboxExporterClouderaLinks', []) %}
{% set local_password = salt['pillar.get']('monitoring:localPassword') %}
{% set scrape_interval_seconds = salt['pillar.get']('monitoring:scrapeIntervalSeconds') %}
{% set cm_metrics_exporter_port = salt['pillar.get']('monitoring:cmMetricsExporterPort', 61010) %}
{% set cm_auto_tls = salt['pillar.get']('monitoring:cmAutoTls', True) %}

{% set request_signer_enabled = salt['pillar.get']('monitoring:requestSignerEnabled', False) %}
{% set request_signer_port = salt['pillar.get']('monitoring:requestSignerPort', 61095) %}
{% if salt['pillar.get']('monitoring:requestSignerUseToken', True) %}
  {% set request_signer_use_token = 'true' %}
{% else %}
  {% set request_signer_use_token = 'false' %}
{% endif %}
{% set request_signer_token_validity_min = salt['pillar.get']('monitoring:requestSignerTokenValidityMin', 60) %}
{% set request_signer_user = salt['pillar.get']('monitoring:requestSignerUser', 'signer') %}

{% set monitoring_access_key_id = salt['pillar.get']('monitoring:monitoringAccessKeyId') %}
{% set monitoring_access_key_secret = salt['pillar.get']('monitoring:monitoringPrivateKey') %}
{% set monitoring_access_key_type = salt['pillar.get']('monitoring:monitoringAccessKeyType', 'Ed25519') %}
{% set remote_write_url = salt['pillar.get']('monitoring:remoteWriteUrl') %}

{% if salt['pillar.get']('telemetry:clusterType') == "datalake" %}
  {% set cm_cluster_type = "BASE_CLUSTER" %}
{% else %}
  {% set cm_cluster_type = "COMPUTE_CLUSTER" %}
{% endif %}
{% set tls_cipher_suites_blackbox_exporter = salt['pillar.get']('monitoring:tlsCipherSuitesBlackBoxExporter') %}

{% do monitoring.update({
    "enabled": monitoring_enabled,
    "is_systemd": is_systemd,
    "type": type,
    "remoteWriteUrl": remote_write_url,
    "scrapeIntervalSeconds": scrape_interval_seconds,
    "cmUsername": cm_username,
    "cmPassword": cm_password,
    "cmClusterType": cm_cluster_type,
    "cmMetricsExporterPort": cm_metrics_exporter_port,
    "cmAutoTls": cm_auto_tls,
    "agentUser": agent_user,
    "agentPort": agent_port,
    "agentMaxDiskUsage": agent_max_disk_usage,
    "retentionMinTime": retention_min_time,
    "retentionMaxTime": retention_max_time,
    "minBackoff": min_backoff,
    "maxBackoff": max_backoff,
    "maxShards": max_shards,
    "maxSamplesPerSend": max_samples_per_send,
    "capacity": capacity,
    "walTruncateFrequency": wal_truncate_frequency,
    "nodeExporterUser": node_exporter_user,
    "nodeExporterPort": node_exporter_port,
    "nodeExporterCollectors": node_exporter_collectors,
    "nodeExporterExists": node_exporter_exists,
    "blackboxExporterUser": blackbox_exporter_user,
    "blackboxExporterPort": blackbox_exporter_port,
    "blackboxExporterCloudIntervalSeconds": blackbox_exporter_cloud_interval_seconds,
    "blackboxExporterClouderaIntervalSeconds": blackbox_exporter_cloudera_interval_seconds,
    "blackboxExporterCheckOnAllNodes": blackbox_exporter_check_on_all_nodes,
    "blackboxExporterExists" : blackbox_exporter_exists,
    "blackboxExporterCloudLinks" : blackbox_exporter_cloud_links,
    "blackboxExporterClouderaLinks" : blackbox_exporter_cloudera_links,
    "localPassword": local_password,
    "requestSignerEnabled" : request_signer_enabled,
    "requestSignerPort" : request_signer_port,
    "requestSignerUseToken" : request_signer_use_token,
    "requestSignerTokenValidityMin" : request_signer_token_validity_min,
    "requestSignerUser" : request_signer_user,
    "monitoringAccessKeyId": monitoring_access_key_id,
    "monitoringPrivateKey": monitoring_access_key_secret,
    "monitoringAccessKeyType": monitoring_access_key_type,
    "tlsCipherSuitesBlackBoxExporter": tls_cipher_suites_blackbox_exporter,
}) %}