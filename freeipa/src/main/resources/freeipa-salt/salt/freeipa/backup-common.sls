/usr/local/bin/freeipa_backup:
  file.managed:
    - source: salt://freeipa/scripts/freeipa_backup
    - user: root
    - group: root
    - mode: 750

/etc/freeipa_backup.conf:
  file.managed:
    - source: salt://freeipa/templates/freeipa_backup.conf.j2
    - template: jinja
    - user: root
    - group: root
    - mode: 640

/usr/local/bin/backup-log-filter.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/backup-log-filter.sh
