{% set telemetry = {} %}

{% set platform = salt['pillar.get']('telemetry:platform') %}

{% set cluster_crn = salt['pillar.get']('telemetry:clusterCrn') %}
{% set cluster_name = salt['pillar.get']('telemetry:clusterName') %}
{% set cluster_version = salt['pillar.get']('telemetry:clusterVersion') %}
{% set cluster_type = salt['pillar.get']('telemetry:clusterType') %}
{% set cluster_owner = salt['pillar.get']('telemetry:clusterOwner') %}
{% set databus_endpoint = salt['pillar.get']('telemetry:databusEndpoint') %}

{% if salt['pillar.get']('telemetry:databusEndpointValidation') %}
    {% set databus_endpoint_validation = True %}
{% else %}
    {% set databus_endpoint_validation = False %}
{% endif %}

{% set anonymization_rules=[] %}
{% if salt['pillar.get']('telemetry:anonymizationRules') %}
{%   set anonymization_rules = salt['pillar.get']('telemetry:anonymizationRules') %}
{% endif %}

{% set logs=[] %}
{% if salt['pillar.get']('telemetry:logs') %}
{%   set logs = salt['pillar.get']('telemetry:logs') %}
{% endif %}

{% set proxy_full_url = None %}
{% set proxy_protocol = None %}
{% if salt['pillar.get']('proxy:host') %}
  {% set proxy_host = salt['pillar.get']('proxy:host') %}
  {% set proxy_port = salt['pillar.get']('proxy:port')|string %}
  {% set proxy_protocol = salt['pillar.get']('proxy:protocol') %}
  {% set proxy_url = proxy_protocol + "://" + proxy_host + ":" + proxy_port %}
  {% if salt['pillar.get']('proxy:user') and salt['pillar.get']('proxy:password') %}
    {% set proxy_user = salt['pillar.get']('proxy:user') %}
    {% set proxy_password = salt['pillar.get']('proxy:password') %}
    {% set proxy_full_url =  proxy_protocol + "://" + proxy_user + ":"+ proxy_password + "@" + proxy_host + ":" + proxy_port %}
  {% else %}
    {% set proxy_full_url = proxy_url %}
  {% endif %}
{% endif %}
{% set no_proxy_hosts = salt['pillar.get']('proxy:noProxyHosts') %}

{% set version_data = namespace(entities=[]) %}
{% for role in grains.get('roles', []) %}
{% if role.startswith("cdp_telemetry_prewarmed") %}
  {% set version_data.entities = version_data.entities + [role.split("cdp_telemetry_prewarmed_v")[1]]%}
{% endif %}
{% endfor %}
{% if version_data.entities|length > 0 %}
{% set cdp_telemetry_version = version_data.entities[0] | int %}
{% else %}
{% set cdp_telemetry_version = 0 %}
{% endif %}

{% set skip_validation = False %}
{% if salt['pillar.get']('telemetry:skipValidation') %}
    {% set skip_validation = True %}
{% endif %}

{% set s3_bucket = salt['pillar.get']('telemetry:s3_bucket') %}
{% set s3_location = salt['pillar.get']('telemetry:s3_location') %}
{% set s3_region = salt['pillar.get']('telemetry:s3_region') %}
{% set adlsv2_storage_account = salt['pillar.get']('telemetry:adlsv2_storage_account') %}
{% set adlsv2_storage_container = salt['pillar.get']('telemetry:adlsv2_storage_container') %}
{% set adlsv2_storage_location = salt['pillar.get']('telemetry:adlsv2_storage_location') %}
{% set gcs_bucket = salt['pillar.get']('telemetry:gcs_bucket') %}
{% set gcs_location = salt['pillar.get']('telemetry:gcs_location') %}

{% set cloud_storage_upload_params = None %}
{% set test_cloud_storage_upload_params = None %}
{% if s3_location %}
  {% set cloud_storage_upload_params = "s3 upload -e -p '" + compressed_file_pattern + "' --location " + s3_location + " --bucket " + s3_bucket +  " --region " + s3_region %}
  {% set test_cloud_storage_upload_params = "s3 upload -e -p /tmp/.test_cloud_storage_upload.txt --location " + s3_location + " --bucket " + s3_bucket +  " --region " + s3_region %}
{% elif adlsv2_storage_location %}
  {% set cloud_storage_upload_params = "abfs upload -p '" + compressed_file_pattern + "' --location " + adlsv2_storage_location + " --account " + adlsv2_storage_account + " --container " + adlsv2_storage_container%}
  {% set test_cloud_storage_upload_params = "abfs upload -p /tmp/.test_cloud_storage_upload.txt --location " + adlsv2_storage_location + " --account " + adlsv2_storage_account + " --container " + adlsv2_storage_container%}
{% elif gcs_location %}
  {% set cloud_storage_upload_params = "gcs upload -p '" + compressed_file_pattern + "' --location " + gcs_location + " --bucket " + gcs_bucket %}
  {% set test_cloud_storage_upload_params = "gcs upload -p /tmp/.test_cloud_storage_upload.txt --location " + gcs_location + " --bucket " + gcs_bucket %}
{% endif %}

{% do telemetry.update({
    "platform": platform,
    "clusterCrn": cluster_crn,
    "clusterName": cluster_name,
    "clusterVersion": cluster_version,
    "clusterType": cluster_type,
    "clusterOwner": cluster_owner,
    "anonymizationRules": anonymization_rules,
    "databusEndpoint": databus_endpoint,
    "databusEndpointValidation": databus_endpoint_validation,
    "cdpTelemetryVersion": cdp_telemetry_version,
    "proxyUrl": proxy_full_url,
    "proxyProtocol": proxy_protocol,
    "noProxyHosts": no_proxy_hosts,
    "logs": logs,
    "skipValidation": skip_validation,
    "cloudStorageUploadParams": cloud_storage_upload_params,
    "testCloudStorageUploadParams": test_cloud_storage_upload_params
}) %}