{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

/opt/salt/scripts/common_utils.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - source: salt://postgresql/scripts/common_utils.sh
    - template: jinja

/opt/salt/scripts/backup_db.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - source: salt://postgresql/upgrade/scripts/backup_db.sh
    - template: jinja

/opt/salt/scripts/restore_db.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - source: salt://postgresql/upgrade/scripts/restore_db.sh
    - template: jinja

/opt/salt/scripts/check_db_connection.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - source: salt://postgresql/upgrade/scripts/check_db_connection.sh
    - template: jinja

set_pgpassword:
  environ.setenv:
    - name: PGPASSWORD
{%- if 'None' != configure_remote_db %}
    - value: {{ pillar['postgres']['clouderamanager']['remote_admin_pw'] }}
{%- else %}
    - value: {{salt['pillar.get']('postgres:upgrade:embeddeddb_password')}}
{%- endif %}
    - update_minion: True
