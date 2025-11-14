/opt/salt/scripts/salt-check.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 750
    - source: salt://saltsyncer/scripts/salt-check.sh

salt_check_cron_job:
  cron.present:
    - name: /opt/salt/scripts/salt-check.sh
    - user: root
    - minute: '*/3'