/etc/cron.hourly/socket-wait-cleanup.sh:
  file.managed:
    - user: root
    - group: root
    - mode: 750
    - source: salt://ccm/scripts/socket-wait-cleanup.sh
    - unless: test ! -f /cdp/bin/reverse-tunnel-values-GATEWAY.sh