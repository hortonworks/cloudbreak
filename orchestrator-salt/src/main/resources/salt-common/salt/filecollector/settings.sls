{%- from 'fluent/settings.sls' import fluent with context %}
{% set filecollector = {} %}

{% set log_folder = salt['pillar.get']('fluent:logFolderName') %}
{% set s3_log_bucket = salt['pillar.get']('fluent:s3LogArchiveBucketName') %}

{% do filecollector.update({
    "logFolder": log_folder,
    "s3LogBucket": s3_log_bucket
}) %}