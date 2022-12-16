logrotate-salt:
  file.managed:
    - name: /etc/logrotate.d/salt
    - source: salt://logrotate/conf/salt
    - user: root
    - group: root
    - mode: 644

logrotate-cdp-prometheus:
  file.managed:
    - name: /etc/logrotate.d/cdp-prometheus
    - source: salt://logrotate/conf/cdp-prometheus
    - user: root
    - group: root
    - mode: 644