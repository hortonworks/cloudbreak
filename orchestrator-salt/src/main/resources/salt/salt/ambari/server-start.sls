{%- from 'ambari/settings.sls' import ambari with context %}

/var/lib/ambari-server/jdbc-drivers:
  cmd.run:
    - name: cp -R /opt/jdbc-drivers /var/lib/ambari-server/jdbc-drivers
    - unless: ls -1 /var/lib/ambari-server/jdbc-drivers

{% if ambari.is_systemd %}

/etc/systemd/system/ambari-server.service:
  file.managed:
    - source: salt://ambari/systemd/ambari-server.service

start-ambari-server:
  module.wait:
    - name: service.systemctl_reload
    - watch:
      - file: /etc/systemd/system/ambari-server.service
  service.running:
    - enable: True
    - name: ambari-server
    - watch:
       - file: /etc/systemd/system/ambari-server.service

{% else %}

# Upstart case

# Avoid concurrency between SysV and Upstart
disable-ambari-server-sysv:
  cmd.run:
    - name: chkconfig ambari-server off
    - onlyif: chkconfig --list ambari-server | grep on

/etc/init/ambari-server.override:
  file.managed:
    - source: salt://ambari/upstart/ambari-server.override

start-ambari-server:
  service.running:
    - enable: True
    - name: ambari-server

{% endif %}

start-service-registration:
  service.running:
    - enable: True
    - name: service-registration