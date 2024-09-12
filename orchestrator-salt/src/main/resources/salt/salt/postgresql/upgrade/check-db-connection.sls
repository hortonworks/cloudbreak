{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

include:
  - postgresql.upgrade

check_db_connection:
  cmd.run:
    - name: /opt/salt/scripts/check_db_connection.sh -h {{salt['pillar.get']('postgres:clouderamanager:remote_db_url')}} -p {{salt['pillar.get']('postgres:clouderamanager:remote_db_port')}} -u {{salt['pillar.get']('postgres:clouderamanager:remote_admin')}}
    - require:
      - sls: postgresql.upgrade
