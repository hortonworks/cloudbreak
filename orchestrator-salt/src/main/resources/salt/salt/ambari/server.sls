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

/opt/ambari-server/ambari-server-init.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/systemd/ambari-server-init.sh
    - mode: 744

set_install_timeout:
  file.replace:
    - name: /etc/ambari-server/conf/ambari.properties
    - pattern: "agent.package.install.task.timeout=1800"
    - repl: "agent.package.install.task.timeout=3600"
    - watch:
      - pkg: ambari-server

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
       - pkg: ambari-server
       - file: /etc/systemd/system/ambari-server.service