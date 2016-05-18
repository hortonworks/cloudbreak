{%- from 'ambari/settings.sls' import ambari with context %}

include:
  - ambari.repo

ambari-agent:
  pkg.latest:
    - require:
      - sls: ambari.repo

/opt/ambari-agent/ambari-agent-init.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/scripts/ambari-agent-init.sh
    - mode: 755

/etc/ambari-agent/conf/internal_hostname.sh:
  file.managed:
    - source: salt://ambari/scripts/internal_hostname.sh
    - mode: 755
    - watch:
      - pkg: ambari-agent

/etc/ambari-agent/conf/public_hostname.sh:
  file.managed:
    - source: salt://ambari/scripts/public_hostname.sh
    - mode: 755
    - watch:
      - pkg: ambari-agent

set_public_hostname_script:
  file.replace:
    - name: /etc/ambari-agent/conf/ambari-agent.ini
    - pattern: "\\[agent\\]"
    - repl: "[agent]\npublic_hostname_script=/etc/ambari-agent/conf/public_hostname.sh"
    - unless: cat /etc/ambari-agent/conf/ambari-agent.ini | grep public_hostname_script
    - watch:
      - file: /etc/ambari-agent/conf/public_hostname.sh
      - pkg: ambari-agent

set_internal_hostname_script:
  file.replace:
    - name: /etc/ambari-agent/conf/ambari-agent.ini
    - pattern: "\\[agent\\]"
    - repl: "[agent]\nhostname_script=/etc/ambari-agent/conf/internal_hostname.sh"
    - unless: cat /etc/ambari-agent/conf/ambari-agent.ini | grep -v public_hostname_script | grep hostname_script
    - watch:
      - file: /etc/ambari-agent/conf/internal_hostname.sh
      - pkg: ambari-agent

{% if ambari.is_systemd %}

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

{% else %}

/etc/init/ambari-agent.conf:
  file.managed:
    - source: salt://ambari/upstart/ambari-agent.conf

start-ambari-agent:
  service.running:
    - enable: True
    - name: ambari-agent
    - watch:
       - pkg: ambari-agent
       - file: /etc/init/ambari-agent.conf

{% endif %}