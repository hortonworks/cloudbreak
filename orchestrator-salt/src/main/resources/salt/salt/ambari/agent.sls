{%- from 'ambari/settings.sls' import ambari with context %}

include:
  - ambari.repo

ambari-agent:
  pkg.latest:
    - require:
      - sls: ambari.repo

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

set_ambari_server_address:
  file.replace:
    - name: /etc/ambari-agent/conf/ambari-agent.ini
    - pattern: "hostname=localhost"
    - repl: "hostname={{ ambari.server_address }}"
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

{% for ip, args in pillar.get('hosts', {}).items() %}
replace_etc_hosts_on_azure_{{ loop.index }}:
  file.replace:
    - name: /etc/hosts
    - pattern: "{{ ip }}\\s *.*"
    - repl: "{{ ip }} {{ args['fqdn'] }} {{ args['hostname'] }}"
    - append_if_not_found: true
    - onlyif:
      - ls -1 /var/lib/waagent
{% endfor %}

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