{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}
{% set local_backup_dir = salt['pillar.get']('backup_restore_config:temp_restore_dir', '/var/tmp') %}

include:
  - postgresql.disaster_recovery

{% if 'None' != configure_remote_db %}
restore_postgresql_db:
  cmd.run:
    - name: /opt/salt/scripts/restore_db.sh {{salt['pillar.get']('disaster_recovery:object_storage_url')}} {{salt['pillar.get']('postgres:clouderamanager:remote_db_url')}} {{salt['pillar.get']('postgres:clouderamanager:remote_db_port')}} {{salt['pillar.get']('postgres:clouderamanager:remote_admin')}} {{salt['pillar.get']('disaster_recovery:ranger_admin_group')}} {{salt['pillar.get']('disaster_recovery:database_name') or ''}} {{local_backup_dir}}
    - require:
        - sls: postgresql.disaster_recovery

{%- else %}
add_root_role_to_database:
  cmd.run:
    - name: createuser root --superuser --login
    - runas: postgres
    # counting failure as a success because if `root` is already there, this command will fail.
    # whether or not `backup_postgresql_db` succeeds is all we really care about.
    - success_retcodes: 1
    - require:
        - sls: postgresql.disaster_recovery

restore_postgresql_db:
  cmd.run:
    - name: /opt/salt/scripts/restore_db.sh {{salt['pillar.get']('disaster_recovery:object_storage_url')}} "" "" "" {{salt['pillar.get']('disaster_recovery:ranger_admin_group')}} {{salt['pillar.get']('disaster_recovery:database_name') or ''}} {{local_backup_dir}}
    - require:
        - cmd: add_root_role_to_database
{% endif %}
