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

{% set version_data = namespace(entities=[]) %}
{% for role in grains.get('roles', []) %}
{% if role.startswith("metering_prewarmed") %}
  {% set version_data.entities = version_data.entities + [role.split("metering_prewarmed_v")[1]]%}
{% endif %}
{% endfor %}
{% if cluster_name and stream_name and stream_name != "Metering" and version_data.entities|length > 0 %}
{% set version = version_data.entities[0] | int %}
{% else %}
{% set version = 1 %}
{% endif %}

{% if salt['pillar.get']('tags:Cloudera-External-Resource-Name') %}
   {% set metered_cluster_crn = salt['pillar.get']('tags:Cloudera-External-Resource-Name') %}
{% elif salt['pillar.get']('tags:cloudera-external-resource-name') %}
   {% set metered_cluster_crn = salt['pillar.get']('tags:cloudera-external-resource-name') %}
{% elif salt['pillar.get']('tags:cloudera-resource-name') %}
    {% set metered_cluster_crn = salt['pillar.get']('tags:Cloudera-Resource-Name') %}
{% elif salt['pillar.get']('tags:cloudera-resource-name') %}
    {% set metered_cluster_crn = salt['pillar.get']('tags:cloudera-resource-name') %}
{% else %}
    {% set metered_cluster_crn = cluster_crn %}
{% endif %}

{% if salt['pillar.get']('tags:Cloudera-Environment-Resource-Name') %}
   {% set metered_env_crn = salt['pillar.get']('tags:Cloudera-Environment-Resource-Name') %}
{% elif salt['pillar.get']('tags:cloudera-environment-resource-name') %}
   {% set metered_env_crn = salt['pillar.get']('tags:cloudera-environment-resource-name') %}
{% else %}
   {% set metered_env_crn = '' %}
{% endif %}

{% if salt['pillar.get']('tags:Cloudera-Creator-Resource-Name') %}
   {% set metered_creator_crn = salt['pillar.get']('tags:Cloudera-Creator-Resource-Name') %}
{% elif salt['pillar.get']('tags:cloudera-creator-resource-name') %}
   {% set metered_creator_crn = salt['pillar.get']('tags:cloudera-creator-resource-name') %}
{% else %}
   {% set metered_creator_crn = '' %}
{% endif %}

{% if salt['pillar.get']('tags:Cloudera-External-Cluster-Name') %}
   {% set metered_cluster_name = salt['pillar.get']('tags:Cloudera-External-Cluster-Name') %}
{% elif salt['pillar.get']('tags:cloudera-external-cluster-name') %}
   {% set metered_cluster_name = salt['pillar.get']('tags:cloudera-external-cluster-name') %}
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
    "meteredClusterName": metered_cluster_name,
    "meteredEnvCrn": metered_env_crn,
    "meteredCreatorCrn": metered_creator_crn
}) %}