{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}


include:
  - postgresql.disaster_recovery

{% if 'None' != configure_remote_db %}
backup_postgresql_db:
  cmd.run:
    - name: /opt/salt/scripts/backup_db.sh {{salt['pillar.get']('platform')}} {{salt['pillar.get']('disaster_recovery:object_storage_url')}} {{salt['pillar.get']('postgres:remote_db_url')}} {{salt['pillar.get']('postgres:remote_db_port')}} {{salt['pillar.get']('postgres:remote_admin')}} {{salt['pillar.get']('disaster_recovery:aws_region')}}
    - require:
      - sls: postgresql.disaster_recovery

{%- else %}
backup_postgresql_db:
  cmd.run:
    - name: /opt/salt/scripts/backup_db.sh {{salt['pillar.get']('platform')}} {{salt['pillar.get']('disaster_recovery:object_storage_url')}} "" "" "" "" "/var/lib/pgsql"
    - runas: postgres
    - require:
      - sls: postgresql.disaster_recovery
{% endif %}
