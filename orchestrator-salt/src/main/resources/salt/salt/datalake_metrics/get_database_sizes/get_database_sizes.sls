{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}
{% set database_names = salt['pillar.get']('backup_restore_config:database_names_for_sizing', '') %}

include:
  - datalake_metrics.get_database_sizes

{% if 'None' != configure_remote_db %}
get_database_sizes:
  cmd.run:
    - name: /opt/salt/scripts/get_database_sizes.sh -h {{salt['pillar.get']('postgres:clouderamanager:remote_db_url')}} -p {{salt['pillar.get']('postgres:clouderamanager:remote_db_port')}} -u {{salt['pillar.get']('postgres:clouderamanager:remote_admin')}} -d "{{database_names}}"
{%- else %}
add_root_role_to_database:
  cmd.run:
    - name: createuser root --superuser --login
    - runas: postgres
    # counting failure as a success because if `root` is already there, this command will fail.
    - success_retcodes: 1

get_database_sizes:
  cmd.run:
    - name: /opt/salt/scripts/get_database_sizes.sh -h "" -p "" -u "" -d "{{database_names}}"
    - require:
      - cmd: add_root_role_to_database
{% endif %}
