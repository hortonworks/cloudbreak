{% set filecollector = {} %}

{% set s3_base_url = salt['pillar.get']('filecollector:s3BaseUrl') %}
{% set adlsv2_base_url = salt['pillar.get']('filecollector:adlsV2BaseUrl') %}
{% set destination = salt['pillar.get']('filecollector:destination') %}
{% set issue = salt['pillar.get']('filecollector:issue') %}
{% set description = salt['pillar.get']('filecollector:description') %}
{% set start_time = salt['pillar.get']('filecollector:startTime') %}
{% set end_time = salt['pillar.get']('filecollector:endTime') %}
{% set description = salt['pillar.get']('filecollector:description') %}
{% set label_filter = salt['pillar.get']('filecollector:labelFilter') %}
{% set additional_logs = salt['pillar.get']('filecollector:additionalLogs') %}
{% set azure_storage_instance_msi = salt['pillar.get']('filecollector:azureInstanceMsi') %}
{% if salt['pillar.get']('filecollector:azureIdBrokerInstanceMsi') %}
    {% set azure_storage_idbroker_instance_msi = salt['pillar.get']('filecollector:azureIdBrokerInstanceMsi') %}
{% else %}
    {% set azure_storage_idbroker_instance_msi = salt['pillar.get']('filecollector:azureInstanceMsi') %}
{% endif %}

{% do filecollector.update({
    "destination": destination,
    "azureInstanceMsi": azure_storage_instance_msi,
    "azureIdBrokerInstanceMsi": azure_storage_idbroker_instance_msi,
    "adlsV2BaseUrl": adlsv2_base_url,
    "s3BaseUrl": s3_base_url,
    "startTime": start_time,
    "endTime": end_time,
    "issue": issue,
    "description": description,
    "labelFilter": label_filter,
    "additionalLogs": additional_logs
}) %}