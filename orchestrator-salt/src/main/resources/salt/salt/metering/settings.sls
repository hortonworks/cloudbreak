{% set metering = {} %}
{% if salt['pillar.get']('metering:enabled') %}
    {% set metering_enabled = True %}
{% else %}
    {% set metering_enabled = False %}
{% endif %}
{% if grains['init'] == 'upstart' %}
    {% set is_systemd = False %}
{% else %}
    {% set is_systemd = True %}
{% endif %}
{% set platform = salt['pillar.get']('metering:platform') %}
{% set cluster_crn = salt['pillar.get']('metering:clusterCrn') %}
{% set service_type = salt['pillar.get']('metering:serviceType') %}
{% set service_version = salt['pillar.get']('metering:serviceVersion') %}

{% do metering.update({
    "is_systemd" : is_systemd,
    "enabled": metering_enabled,
    "clusterCrn": cluster_crn,
    "serviceType": service_type,
    "serviceVersion": service_version
}) %}