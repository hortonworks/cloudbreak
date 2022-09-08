logrotate-freeipa-healthagent-getcert:
  file.managed:
    - name: /etc/logrotate.d/freeipa-healthagent-getcert
    - source: salt://logrotate/conf/freeipa-healthagent-getcert
    - user: root
    - group: root
    - mode: 644

logrotate-freeipa-healthagent-setup:
  file.managed:
    - name: /etc/logrotate.d/freeipa-healthagent-setup
    - source: salt://logrotate/conf/freeipa-healthagent-setup
    - user: root
    - group: root
    - mode: 644

logrotate-httpd-crt-tracking:
  file.managed:
    - name: /etc/logrotate.d/freeipa-healthagent-setup
    - source: salt://logrotate/conf/freeipa-healthagent-setup
    - user: root
    - group: root
    - mode: 644

logrotate-krb5kdc:
  file.managed:
    - name: /etc/logrotate.d/krb5kdc
    - source: salt://logrotate/conf/krb5kdc
    - user: root
    - group: root
    - mode: 644

logrotate-ipabackup:
  file.managed:
    - name: /etc/logrotate.d/ipabackup
    - source: salt://logrotate/conf/ipabackup
    - user: root
    - group: root
    - mode: 644

logrotate-pkidebug:
  file.managed:
    - name: /etc/logrotate.d/pkidebug
    - source: salt://logrotate/conf/pkidebug
    - user: root
    - group: root
    - mode: 644

logrotate-salt:
  file.managed:
    - name: /etc/logrotate.d/salt
    - source: salt://logrotate/conf/salt
    - user: root
    - group: root
    - mode: 644