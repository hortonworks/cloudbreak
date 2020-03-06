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

{% set platform = salt['pillar.get']('monitoring:platform') %}
{% set type = salt['pillar.get']('monitoring:type') %}
{% set cm_username = salt['pillar.get']('monitoring:cmUsername') %}
{% set cm_password = salt['pillar.get']('monitoring:cmPassword') %}

{% if salt['pillar.get']('monitoring:clusterType') == "datalake" %}
  {% set cm_cluster_type = "BASE_CLUSTER" %}
{% else %}
  {% set cm_cluster_type = "COMPUTE_CLUSTER" %}
{% endif %}

{% set cluster_crn = salt['pillar.get']('monitoring:clusterCrn') %}
{% set cluster_name = salt['pillar.get']('monitoring:clusterName') %}
{% set cluster_version = salt['pillar.get']('monitoring:clusterVersion') %}
{% set cluster_type = salt['pillar.get']('monitoring:clusterType') %}
{% set cluster_owner = salt['pillar.get']('monitoring:clusterOwner') %}

{% do monitoring.update({
    "enabled": monitoring_enabled,
    "is_systemd": is_systemd,
    "type": type,
    "platform": platform,
    "cmUsername": cm_username,
    "cmPassword": cm_password,
    "cmClusterType": cm_cluster_type,
    "clusterCrn": cluster_crn,
    "clusterName": cluster_name,
    "clusterVersion": cluster_version,
    "clusterType": cluster_type,
    "clusterOwner": cluster_owner
}) %}