{%- from 'ambari/settings.sls' import ambari with context %}

{% if not ambari.is_predefined_repo %}

include:
  - ambari.repo

haveged:
  pkg.installed: []
  service.running:
    - enable: True

ambari-server:
  pkg.latest:
    - require:
      - sls: ambari.repo

{% endif %}

/var/lib/ambari-server/jdbc-drivers:
  cmd.run:
    - name: cp -R /opt/jdbc-drivers /var/lib/ambari-server/jdbc-drivers
    - unless: ls -1 /var/lib/ambari-server/jdbc-drivers

/opt/ambari-server/ambari-server-init.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/scripts/ambari-server-init.sh
    - mode: 744

set_install_timeout:
  file.replace:
    - name: /etc/ambari-server/conf/ambari.properties
    - pattern: "agent.package.install.task.timeout=1800"
    - repl: "agent.package.install.task.timeout=3600"

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

/etc/init/ambari-server.conf:
  file.managed:
    - source: salt://ambari/upstart/ambari-server.conf

start-ambari-server:
  service.running:
    - enable: True
    - name: ambari-server
    - watch:
       - file: /etc/init/ambari-server.conf

{% endif %}