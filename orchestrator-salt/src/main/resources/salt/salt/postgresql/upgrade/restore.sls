{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}
{% set backup_dir = salt['pillar.get']('upgrade:backup:directory', 'None') %}

include:
  - postgresql.upgrade

{{ backup_dir }}/passwords.sql:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 600
    - source: salt://postgresql/upgrade/scripts/passwords.j2
    - template: jinja

restore_postgresql_db:
  cmd.run:
{%- if 'None' != configure_remote_db %}
    - name: /opt/salt/scripts/restore_db.sh -h {{salt['pillar.get']('postgres:clouderamanager:remote_db_url')}} -p {{salt['pillar.get']('postgres:clouderamanager:remote_db_port')}} -u {{salt['pillar.get']('postgres:clouderamanager:remote_admin')}}
{%- else %}
    - name: /opt/salt/scripts/restore_db.sh -h {{salt['pillar.get']('postgres:upgrade:embeddeddb_host')}} -p {{salt['pillar.get']('postgres:upgrade:embeddeddb_port')}} -u {{salt['pillar.get']('postgres:upgrade:embeddeddb_user')}}
{%- endif %}
    - env:
      - VERSION: {{salt['pillar.get']('postgres:upgrade:target_version')}}
      - IS_REMOTE_DB: {{ configure_remote_db }}
    - require:
      - sls: postgresql.upgrade
