include:
  - freeipa.backup-common

{% if salt['pillar.get']('freeipa:backup:enabled') %}
freeipa_backup_check:
  cmd.run:
    - name: /usr/local/bin/freeipa_backup -pl
    - require:
        - file: /usr/local/bin/freeipa_backup
        - file: /usr/local/bin/backup-log-filter.sh
        - file: /etc/freeipa_backup.conf
{% endif %}
