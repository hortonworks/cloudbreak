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

set_pgpassword:
  environ.setenv:
    - name: PGPASSWORD
    - value: {{ pillar['postgres']['clouderamanager']['remote_admin_pw'] }}
    - update_minion: True
