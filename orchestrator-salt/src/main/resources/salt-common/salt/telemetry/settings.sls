{% set telemetry = {} %}

{% set platform = salt['pillar.get']('telemetry:platform') %}

{% set cluster_crn = salt['pillar.get']('telemetry:clusterCrn') %}
{% set cluster_name = salt['pillar.get']('telemetry:clusterName') %}
{% set cluster_version = salt['pillar.get']('telemetry:clusterVersion') %}
{% set cluster_type = salt['pillar.get']('telemetry:clusterType') %}
{% set cluster_owner = salt['pillar.get']('telemetry:clusterOwner') %}

{% set anonymization_rules=[] %}
{% if salt['pillar.get']('telemetry:anonymizationRules') %}
{%   set anonymization_rules = salt['pillar.get']('telemetry:anonymizationRules') %}
{% endif %}

{% set logs=[] %}
{% if salt['pillar.get']('telemetry:logs') %}
{%   set logs = salt['pillar.get']('telemetry:logs') %}
{% endif %}

{% do telemetry.update({
    "platform": platform,
    "clusterCrn": cluster_crn,
    "clusterName": cluster_name,
    "clusterVersion": cluster_version,
    "clusterType": cluster_type,
    "clusterOwner": cluster_owner,
    "anonymizationRules": anonymization_rules,
    "logs": logs
}) %}