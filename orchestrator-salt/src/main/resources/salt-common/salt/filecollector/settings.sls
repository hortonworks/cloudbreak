{% set filecollector = {} %}

{% set s3_bucket = salt['pillar.get']('filecollector:s3_bucket') %}
{% set s3_location = salt['pillar.get']('filecollector:s3_location') %}
{% set s3_region = salt['pillar.get']('filecollector:s3_region') %}
{% set adlsv2_storage_account = salt['pillar.get']('filecollector:adlsv2_storage_account') %}
{% set adlsv2_storage_container = salt['pillar.get']('filecollector:adlsv2_storage_container') %}
{% set adlsv2_storage_location = salt['pillar.get']('filecollector:adlsv2_storage_location') %}
{% set destination = salt['pillar.get']('filecollector:destination') %}
{% set issue = salt['pillar.get']('filecollector:issue') %}
{% set description = salt['pillar.get']('filecollector:description') %}
{% set start_time = salt['pillar.get']('filecollector:startTime') %}
{% set end_time = salt['pillar.get']('filecollector:endTime') %}
{% set label_filter = salt['pillar.get']('filecollector:labelFilter') %}
{% set include_salt_logs = salt['pillar.get']('filecollector:includeSaltLogs') %}
{% set update_package = salt['pillar.get']('filecollector:updatePackage') %}
{% set additional_logs = salt['pillar.get']('filecollector:additionalLogs') %}

{% if s3_location and not s3_region %}
  {%- set instanceDetails = salt.cmd.run('curl -s http://169.254.169.254/latest/dynamic/instance-identity/document') | load_json %}
  {%- set s3_region = instanceDetails['region'] %}
{% endif %}

{% set cloud_storage_upload_params = None %}
{% if s3_location %}
  {% set cloud_storage_upload_params = "s3 upload -e -p '/var/lib/filecollector/*.gz' --location " + s3_location + " --bucket " + s3_bucket +  " --region " + s3_region %}
{% elif adlsv2_storage_location %}
  {% set cloud_storage_upload_params = "abfs upload -p '/var/lib/filecollector/*.gz' --location " + adlsv2_storage_location + " --account " + adlsv2_storage_account + " --container " + adlsv2_storage_container%}
{% endif %}

{% do filecollector.update({
    "destination": destination,
    "cloudStorageUploadParams": cloud_storage_upload_params,
    "startTime": start_time,
    "endTime": end_time,
    "issue": issue,
    "description": description,
    "labelFilter": label_filter,
    "additionalLogs": additional_logs,
    "includeSaltLogs": include_salt_logs,
    "updatePackage" : update_package
}) %}