{% set monitoring = {} %}

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

{% set use_dev_stack = salt['pillar.get']('monitoring:useDevStack') %}
{% set type = salt['pillar.get']('monitoring:type') %}
{% set cm_username = salt['pillar.get']('monitoring:cmUsername') %}
{% set cm_password = salt['pillar.get']('monitoring:cmPassword') %}
{% set username = salt['pillar.get']('monitoring:username') %}
{% set password = salt['pillar.get']('monitoring:password') %}
{% set token = salt['pillar.get']('monitoring:token') %}
{% set scrape_interval_seconds = salt['pillar.get']('monitoring:scrapeIntervalSeconds') %}
{% set cm_metrics_exporter_port = salt['pillar.get']('monitoring:cmMetricsExporterPort', 61010) %}

{%- if use_dev_stack %}
  {%- if salt['pillar.get']('cloudera-manager:address') %}
    {% set remote_write_url = "http://" + salt['pillar.get']('cloudera-manager:address') + ":9090/api/v1/write" %}
  {%- else %}
    {% set remote_write_url = "http://" + salt['grains.get']('master')[0] + ":9090/api/v1/write" %}
  {%- endif %}
{%- else %}
  {% set remote_write_url = salt['pillar.get']('monitoring:remoteWriteUrl') %}
{%- endif %}

{% if salt['pillar.get']('telemetry:clusterType') == "datalake" %}
  {% set cm_cluster_type = "BASE_CLUSTER" %}
{% else %}
  {% set cm_cluster_type = "COMPUTE_CLUSTER" %}
{% endif %}

{% do monitoring.update({
    "enabled": monitoring_enabled,
    "is_systemd": is_systemd,
    "type": type,
    "username": username,
    "password": password,
    "token": token,
    "remoteWriteUrl": remote_write_url,
    "scrapeIntervalSeconds": scrape_interval_seconds,
    "cmUsername": cm_username,
    "cmPassword": cm_password,
    "cmClusterType": cm_cluster_type,
    "cmMetricsExporterPort": cm_metrics_exporter_port,
    "useDevStack": use_dev_stack
}) %}