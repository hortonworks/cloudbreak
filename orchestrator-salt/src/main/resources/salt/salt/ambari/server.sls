{%- from 'ambari/settings.sls' import ambari with context %}

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
    - source: salt://ambari/scripts/ambari-server-init.sh
    - mode: 744

set_install_timeout:
  file.replace:
    - name: /etc/ambari-server/conf/ambari.properties
    - pattern: "agent.package.install.task.timeout=1800"
    - repl: "agent.package.install.task.timeout=3600"
    - watch:
      - pkg: ambari-server

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

{% else %}

/etc/init/ambari-server.conf:
  file.managed:
    - source: salt://ambari/upstart/ambari-server.conf

start-ambari-server:
  service.running:
    - enable: True
    - name: ambari-server
    - watch:
       - pkg: ambari-server
       - file: /etc/init/ambari-server.conf

{% endif %}