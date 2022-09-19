{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

upgrade:
  backup:
    compressed_file_name: rds_backup.tar.gz
    logfile: /var/log/postgres_upgrade_backup.log
{%- if 'None' != configure_remote_db %}
    directory: /var/tmp/postges_upgrade_backup
{%- else %}
    directory: /dbfs/postgres_upgrade_backup
{%- endif %}
  restore:
    logfile: /var/log/postgres_upgrade_restore.log