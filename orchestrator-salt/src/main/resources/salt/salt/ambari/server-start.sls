{%- from 'ambari/settings.sls' import ambari with context %}

copy-optional-jdbc-drivers:
  cmd.run:
    - name: cp -R /opt/jdbc-drivers /usr/share/java
    - onlyif: test -d /opt/jdbc-drivers

{% if ambari.ambari_database.ambariVendor == 'embedded' %}
ambari-start-postgresql:
  service.running:
    - enable: True
    - name: postgresql
{% endif %}

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