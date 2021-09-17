/opt/salt/scripts/backup_db.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - source: salt://postgresql/disaster_recovery/scripts/backup_db.sh
    - template: jinja

/opt/salt/scripts/restore_db.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - source: salt://postgresql/disaster_recovery/scripts/restore_db.sh
    - template: jinja

/opt/salt/scripts/backup_and_restore_dh.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - source: salt://postgresql/disaster_recovery/scripts/backup_and_restore_dh.sh
    - template: jinja

/opt/salt/postgresql/.pgpass:
  file.managed:
    - makedirs: True
    - mode: 600
    - source: salt://postgresql/disaster_recovery/.pgpass
    - template: jinja

set_pgpass_file:
  environ.setenv:
    - name: PGPASSFILE
    - value: /opt/salt/postgresql/.pgpass
    - require:
      - file: /opt/salt/postgresql/.pgpass
