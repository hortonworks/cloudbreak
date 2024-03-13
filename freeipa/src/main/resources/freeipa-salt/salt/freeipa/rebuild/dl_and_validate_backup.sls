{%- set full_backup_location = salt['pillar.get']('freeipa:rebuild:full_backup_location') %}
{%- set data_backup_location = salt['pillar.get']('freeipa:rebuild:data_backup_location') %}

{%- do salt.log.debug("log_full_backup_location " ~ full_backup_location) %}
{%- do salt.log.debug("log_data_backup_location " ~ data_backup_location) %}

include:
  - freeipa.backup-common

download_and_validate_backup:
  cmd.run:
    - name: /usr/local/bin/freeipa_dl_and_validate_backup.sh
    - failhard: True
    - env:
        - FULL_BACKUP_LOCATION: {{ full_backup_location }}
        - DATA_BACKUP_LOCATION: {{ data_backup_location }}