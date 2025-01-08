{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

{% set canary_db_url= salt['pillar.get']('postgresql-upgrade:upgrade:checkconnection:canary_hostname') %}
{% set canary_username= salt['pillar.get']('postgresql-upgrade:upgrade:checkconnection:canary_username') %}

{% set remote_db_url = salt['pillar.get']('postgres:clouderamanager:remote_db_url') %}
{% set remote_db_port = salt['pillar.get']('postgres:clouderamanager:remote_db_port') %}
{% set remote_admin = salt['pillar.get']('postgres:clouderamanager:remote_admin') %}
include:
  - postgresql.upgrade

check_db_connection:
  cmd.run:
    - name: /opt/salt/scripts/check_db_connection.sh -h {{ canary_db_url or remote_db_url }} -p {{ remote_db_port }} -u {{ canary_username or remote_admin }}
    - failhard: True
    - require:
      - sls: postgresql.upgrade