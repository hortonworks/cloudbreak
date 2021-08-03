include:
  - freeipa.backup-common

{% if salt['pillar.get']('freeipa:backup:enabled') %}
freeipa_full_backup:
  cmd.run:
    - name: /usr/local/bin/freeipa_backup -t FULL -f "{{salt['grains.get']('fqdn')}}/full" && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/freeipa_full_backup-executed
    - failhard: True
    - require:
        - file: /usr/local/bin/freeipa_backup
        - file: /usr/local/bin/backup-log-filter.sh
        - file: /etc/freeipa_backup.conf
{% endif %}