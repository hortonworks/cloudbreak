{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

include:
  - postgresql.disaster_recovery
{ %- if salt[ 'pillar.get' ]('postgres:postgres_version', '10') | int == 11 % }
  - postgresql.pg11-alternatives
{ %- endif % }

{% if 'None' != configure_remote_db %}
backup_postgresql_db:
  cmd.run:
    - name: /opt/salt/scripts/backup_db.sh {{salt['pillar.get']('disaster_recovery:object_storage_url')}} {{salt['pillar.get']('postgres:clouderamanager:remote_db_url')}} {{salt['pillar.get']('postgres:clouderamanager:remote_db_port')}} {{salt['pillar.get']('postgres:clouderamanager:remote_admin')}} {{salt['pillar.get']('disaster_recovery:ranger_admin_group')}} {{salt['pillar.get']('disaster_recovery:close_connections')}} {{salt['pillar.get']('disaster_recovery:database_name') or ''}}
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

backup_postgresql_db:
  cmd.run:
    - name: /opt/salt/scripts/backup_db.sh {{salt['pillar.get']('disaster_recovery:object_storage_url')}} "" "" "" {{salt['pillar.get']('disaster_recovery:ranger_admin_group')}} {{salt['pillar.get']('disaster_recovery:close_connections')}} {{salt['pillar.get']('disaster_recovery:database_name') or ''}}
    - require:
      - cmd: add_root_role_to_database
{% endif %}
