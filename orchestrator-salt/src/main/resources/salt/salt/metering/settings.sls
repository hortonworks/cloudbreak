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
{% set cluster_name = salt['pillar.get']('metering:clusterName') %}
{% set service_type = salt['pillar.get']('metering:serviceType') %}
{% set service_version = salt['pillar.get']('metering:serviceVersion') %}
{% set stream_name = salt['pillar.get']('metering:streamName') %}

{% if cluster_name and stream_name and stream_name != "Metering" %}
    {% if "metering_prewarmed_v2" in grains.get('roles', []) or not salt['file.directory_exists' ]('/etc/metering') %}
        {% set version = 2 %}
    {% else %}
        {% set version = 1 %}
    {% endif %}
{% else %}
    {% set version = 1 %}
{% endif %}

{% if salt['pillar.get']('tags:Cloudera-External-Resource-Name') %}
   {% set metered_cluster_crn = salt['pillar.get']('tags:Cloudera-External-Resource-Name') %}
{% elif salt['pillar.get']('tags:Cloudera-Resource-Name') %}
    {% set metered_cluster_crn = salt['pillar.get']('tags:Cloudera-Resource-Name') %}
{% else %}
    {% set metered_cluster_crn = cluster_crn %}
{% endif %}
{% if salt['pillar.get']('tags:Cloudera-External-Cluster-Name') %}
   {% set metered_cluster_name = salt['pillar.get']('tags:Cloudera-External-Cluster-Name') %}
{% else %}
   {% set metered_cluster_name = cluster_name %}
{% endif %}

{% if salt['pillar.get']('tags:Cloudera-Service-Feature') %}
  {% set service_feature = salt['pillar.get']('tags:Cloudera-Service-Feature') %}
{% else %}
  {% set service_feature = None %}
{% endif %}

{% do metering.update({
    "is_systemd" : is_systemd,
    "enabled": metering_enabled,
    "clusterCrn": cluster_crn,
    "clusterName": cluster_name,
    "serviceType": service_type,
    "serviceFeature": service_feature,
    "serviceVersion": service_version,
    "version": version,
    "meteredClusterCrn": metered_cluster_crn,
    "meteredClusterName": metered_cluster_name
}) %}