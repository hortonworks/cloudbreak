{% set fluent = {} %}
{% if salt['pillar.get']('fluent:enabled') %}
    {% set fluent_enabled = True %}
{% else %}
    {% set fluent_enabled = False %}
{% endif %}
{% set fluent_user = salt['pillar.get']('fluent:user') %}
{% set fluent_group = salt['pillar.get']('fluent:group') %}
{% set provider_prefix = salt['pillar.get']('fluent:providerPrefix') %}
{% set s3_log_bucket = salt['pillar.get']('fluent:s3LogArchiveBucketName') %}
{% set s3_log_folder = salt['pillar.get']('fluent:s3LogFolderName') %}
{% set partition_interval = salt['pillar.get']('fluent:partitionIntervalMin') %}

{% do fluent.update({
    "enabled": fluent_enabled,
    "user": fluent_user,
    "group": fluent_group,
    "providerPrefix": provider_prefix,
    "partitionIntervalMin": partition_interval,
    "s3LogArchiveBucketName" : s3_log_bucket,
    "s3LogFolderName": s3_log_folder
}) %}