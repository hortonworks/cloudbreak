{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}
{% set object_storage_url = salt['pillar.get']('disaster_recovery:object_storage_url') %}
{% set remote_db_url = salt['pillar.get']('postgres:clouderamanager:remote_db_url') %}
{% set remote_db_port = salt['pillar.get']('postgres:clouderamanager:remote_db_port') %}
{% set remote_admin = salt['pillar.get']('postgres:clouderamanager:remote_admin') %}
{% set ranger_admin_group = salt['pillar.get']('disaster_recovery:ranger_admin_group') %}
{% set close_connections = salt['pillar.get']('disaster_recovery:close_connections') %}
{% set compression_level = salt['pillar.get']('disaster_recovery:compression_level', '0') %}
{% set database_name = salt['pillar.get']('disaster_recovery:database_name', '') %}
{% set postgres_version = salt['pillar.get']('postgres:postgres_version', '10') | int %}

include:
  - postgresql.disaster_recovery
{%- if postgres_version in [11, 14, 17] %}
  - postgresql.pg-alternatives
{%- endif %}

{% if 'None' != configure_remote_db %}
backup_postgresql_db:
  cmd.run:
    - name: /opt/salt/scripts/backup_db.sh -s {{object_storage_url}} -h {{remote_db_url}} -p {{remote_db_port}} -u {{remote_admin}} -r {{ranger_admin_group}} -c {{close_connections}} -z {{compression_level}} {{database_name}}
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
    - name: /opt/salt/scripts/backup_db.sh -s {{object_storage_url}} -r {{ranger_admin_group}} -c {{close_connections}} -z {{compression_level}} {{database_name}}
    - require:
      - cmd: add_root_role_to_database
{% endif %}
