{%- if not salt['file.file_exists']('/usr/local/bin/backup-log-filter.sh') %}
/usr/local/bin/backup-log-filter.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://cdpluksvolumebackup/scripts/backup-log-filter
{%- endif %}
/etc/cdp_luks_volume_backup.conf:
  file.managed:
    - source: salt://cdpluksvolumebackup/templates/cdp_luks_volume_backup.conf.j2
    - template: jinja
    - user: root
    - group: root
    - mode: 640
/usr/local/bin/cdp_luks_volume_backup:
  file.managed:
    - source: salt://cdpluksvolumebackup/scripts/cdp_luks_volume_backup
    - user: root
    - group: root
    - mode: 750
/etc/cron.daily/cdp_luks_volume_backup_daily:
  file.managed:
    - source: salt://cdpluksvolumebackup/scripts/cdp_luks_volume_backup_daily
    - user: root
    - group: root
    - mode: 750


