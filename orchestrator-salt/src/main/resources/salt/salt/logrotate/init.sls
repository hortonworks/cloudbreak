logrotate-salt:
  file.managed:
    - name: /etc/logrotate.d/salt
    - source: salt://logrotate/conf/salt
    - user: root
    - group: root
    - mode: 644