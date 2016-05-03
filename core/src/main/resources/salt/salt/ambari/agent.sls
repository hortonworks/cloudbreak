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
    - mode: 755

/etc/systemd/system/ambari-agent.service:
  file.managed:
    - source: salt://ambari/systemd/ambari-agent.service

/etc/ambari-agent/conf/internal_hostname.sh:
  file.managed:
    - source: salt://ambari/scripts/internal_hostname.sh
    - mode: 755
    - watch:
      - pkg: ambari-agent

/etc/ambari-agent/conf/ambari-agent.ini:
  file.replace:
    - pattern: "\\[agent\\]"
    - repl: "[agent]\nhostname_script=/etc/ambari-agent/conf/internal_hostname.sh"
    - unless: cat /etc/ambari-agent/conf/ambari-agent.ini | grep hostname_script
    - watch:
      - file: /etc/ambari-agent/conf/internal_hostname.sh
      - pkg: ambari-agent

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