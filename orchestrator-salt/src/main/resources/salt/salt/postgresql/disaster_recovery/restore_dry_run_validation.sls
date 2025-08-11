{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}
{% set object_storage_url = salt['pillar.get']('disaster_recovery:object_storage_url') %}
{% set remote_db_url = salt['pillar.get']('postgres:clouderamanager:remote_db_url') %}
{% set remote_db_port = salt['pillar.get']('postgres:clouderamanager:remote_db_port') %}
{% set remote_admin = salt['pillar.get']('postgres:clouderamanager:remote_admin') %}
{% set database_name = salt['pillar.get']('disaster_recovery:database_name', '') %}
{% set raz_enabled = salt['pillar.get']('disaster_recovery:raz_enabled', '') %}
{% set local_backup_dir = salt['pillar.get']('backup_restore_config:temp_restore_dir', '/var/tmp') %}

include:
  - postgresql.disaster_recovery

{% if 'None' != configure_remote_db %}
backup_restore_dry_run:
  cmd.run:
    - name: /opt/salt/scripts/restore_dry_run_validation.sh {{object_storage_url}} {{remote_db_url}} {{remote_db_port}} {{remote_admin}} {{database_name}} {{raz_enabled}} {{local_backup_dir}}
    - require:
        - sls: postgresql.disaster_recovery

{%- else %}
add_root_role_to_database:
  cmd.run:
    - name: createuser root --superuser --login
    - runas: postgres
    - success_retcodes: 1
    - require:
        - sls: postgresql.disaster_recovery

backup_restore_dry_run:
  cmd.run:
    - name: /opt/salt/scripts/restore_dry_run_validation.sh {{object_storage_url}} "" "" "" {{database_name}} {{raz_enabled}} {{local_backup_dir}}
    - require:
        - cmd: add_root_role_to_database
{% endif %}
