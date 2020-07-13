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

{% set type = salt['pillar.get']('monitoring:type') %}
{% set cm_username = salt['pillar.get']('monitoring:cmUsername') %}
{% set cm_password = salt['pillar.get']('monitoring:cmPassword') %}

{% if salt['pillar.get']('telemetry:clusterType') == "datalake" %}
  {% set cm_cluster_type = "BASE_CLUSTER" %}
{% else %}
  {% set cm_cluster_type = "COMPUTE_CLUSTER" %}
{% endif %}

{% do monitoring.update({
    "enabled": monitoring_enabled,
    "is_systemd": is_systemd,
    "type": type,
    "cmUsername": cm_username,
    "cmPassword": cm_password,
    "cmClusterType": cm_cluster_type
}) %}