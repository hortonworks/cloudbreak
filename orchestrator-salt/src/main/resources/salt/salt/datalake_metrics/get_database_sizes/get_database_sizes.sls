{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

include:
  - datalake_metrics.get_database_sizes

{% if 'None' != configure_remote_db %}
get_database_sizes:
  cmd.run:
    - name: /opt/salt/scripts/get_database_sizes.sh {{salt['pillar.get']('postgres:clouderamanager:remote_db_url')}} {{salt['pillar.get']('postgres:clouderamanager:remote_db_port')}} {{salt['pillar.get']('postgres:clouderamanager:remote_admin')}}
{%- else %}
add_root_role_to_database:
  cmd.run:
    - name: createuser root --superuser --login
    - runas: postgres
    # counting failure as a success because if `root` is already there, this command will fail.
    - success_retcodes: 1

get_database_sizes:
  cmd.run:
    - name: /opt/salt/scripts/get_database_sizes.sh "" "" ""
    - require:
      - cmd: add_root_role_to_database
{% endif %}
