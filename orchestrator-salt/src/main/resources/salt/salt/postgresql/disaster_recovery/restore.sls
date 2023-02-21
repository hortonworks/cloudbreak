{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

include:
  - postgresql.disaster_recovery

{% set storage = salt['pillar.get']('disaster_recovery:object_storage_url') %}
{% set host = salt['pillar.get']('postgres:clouderamanager:remote_db_url') %}
{% set port = salt['pillar.get']('postgres:clouderamanager:remote_db_port') %}
{% set username = salt['pillar.get']('postgres:clouderamanager:remote_admin') %}
{% set group = salt['pillar.get']('disaster_recovery:ranger_admin_group') %}
{% set dbname = salt['pillar.get']('disaster_recovery:database_name') %}
{% if dbname %}
{% set dbname_list = dbname.split(' ') %}
{% endif %}

{% if 'None' != configure_remote_db %}
restore_postgresql_db:
  cmd.run:
    - name: /opt/salt/scripts/restore_db.sh{% if storage %} -s {{storage}}{% endif %}{% if host %} -h {{host}}{% endif %}{% if port %} -p {{port}}{% endif %}{% if username %} -u {{username}}{% endif %}{% if group %} -g {{group}}{% endif %}{% if dbname %}{% for item in dbname_list %} -n {{item}}{% endfor %}{% endif %}
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
    - name: /opt/salt/scripts/restore_db.sh{% if storage %} -s {{storage}}{% endif %} -h "" -p "" -u ""{% if group %} -g {{group}}{% endif %}{% if dbname %}{% for item in dbname_list %} -n {{item}}{% endfor %}{% endif %}
    - require:
        - cmd: add_root_role_to_database
{% endif %}
