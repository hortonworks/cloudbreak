/usr/local/bin/freeipa_backup:
  file.managed:
    - source: salt://freeipa/scripts/freeipa_backup
    - user: root
    - group: root
    - mode: 750

{% if salt['pillar.get']('freeipa:backup:enabled') %}
/etc/freeipa_backup.conf:
  file.managed:
    - source: salt://freeipa/templates/freeipa_backup.conf.j2
    - template: jinja
    - user: root
    - group: root
    - mode: 640

{% if salt['pillar.get']('freeipa:backup:monthly_full_enabled') %}
/etc/cron.monthly/freeipa_backup_monthly:
  file.managed:
    - source: salt://freeipa/templates/freeipa_backup_monthly.j2
    - template: jinja
    - user: root
    - group: root
    - mode: 750
{% endif %}

{% if salt['pillar.get']('freeipa:backup:hourly_enabled') %}
/etc/cron.hourly/freeipa_backup_hourly:
  file.managed:
    - source: salt://freeipa/templates/freeipa_backup_hourly.j2
    - template: jinja
    - user: root
    - group: root
    - mode: 750
{% endif %}

{% if salt['pillar.get']('freeipa:backup:initial_full_enabled') %}
freeipa_initial_full_backup:
  cmd.run:
    - name: /usr/local/bin/freeipa_backup -t FULL -f "{{salt['grains.get']('fqdn')}}/full" && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/freeipa_initial_backup-executed
    - unless: test -f /var/log/freeipa_initial_backup-executed
    - require:
        - file: /usr/local/bin/freeipa_backup
        - file: /etc/freeipa_backup.conf
{% endif %}
{% endif %}
