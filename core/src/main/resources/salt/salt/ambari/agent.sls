include:
  - ambari.repo

ambari-agent:
  pkg.latest:
    - require:
      - sls: ambari.repo

/opt/ambari-agent/ambari-agent-init.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/systemd/ambari-agent-init.sh
    - mode: 744

/etc/systemd/system/ambari-agent.service:
  file.managed:
    - source: salt://ambari/systemd/ambari-agent.service

start-ambari-agent:
  module.wait:
    - name: service.systemctl_reload
    - watch:
      - file: /etc/systemd/system/ambari-agent.service
  service.running:
    - enable: True
    - name: ambari-agent
    - watch:
        - pkg: ambari-agent
        - file: /etc/systemd/system/ambari-agent.service