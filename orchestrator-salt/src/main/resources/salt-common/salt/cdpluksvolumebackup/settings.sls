{% set cdpluksvolumebackup = {} %}
{% set backup_location = salt['pillar.get']('cdpluksvolumebackup:backup_location') %}
{% set aws_region = salt['pillar.get']('cdpluksvolumebackup:aws_region') %}

{% do cdpluksvolumebackup.update({
 "backup_location" : backup_location,
 "aws_region": aws_region
}) %}
