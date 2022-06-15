{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

include:
  - postgresql.upgrade

restore_postgresql_db:
  cmd.run:
    - name: /opt/salt/scripts/restore_db.sh -h {{salt['pillar.get']('postgres:clouderamanager:remote_db_url')}} -p {{salt['pillar.get']('postgres:clouderamanager:remote_db_port')}} -u {{salt['pillar.get']('postgres:clouderamanager:remote_admin')}}
    - require:
        - sls: postgresql.upgrade