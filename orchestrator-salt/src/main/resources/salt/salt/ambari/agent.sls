{%- from 'ambari/settings.sls' import ambari with context %}
{%- from 'nodes/settings.sls' import host with context %}

{% if not ambari.is_predefined_repo %}

include:
  - ambari.repo

ambari-agent:
  pkg.installed:
    - require:
      - sls: ambari.repo
    - version: {{ ambari.version }}

{% else %}

parallel_task_execution:
  file.replace:
    - name: /etc/ambari-agent/conf/ambari-agent.ini
    - pattern: parallel_execution=0
    - repl: parallel_execution=1

reduce_reconnect_retry_delay:
  file.replace:
    - name: /etc/ambari-agent/conf/ambari-agent.ini
    - pattern: "max_reconnect_retry_delay.*=.*30"
    - repl: max_reconnect_retry_delay=10

reduce_connect_retry_delay:
  file.replace:
    - name: /etc/ambari-agent/conf/ambari-agent.ini
    - pattern: "connect_retry_delay.*=.*10"
    - repl: connect_retry_delay=5

{% endif %}

/etc/environment:
  file.append:
    - text: "SPARK_CLASSPATH=${SPARK_CLASSPATH}:/usr/lib/hadoop/lib/*"
    - unless: cat /etc/environment | grep SPARK_CLASSPATH

/etc/ambari-agent/conf/internal_hostname.sh:
  file.managed:
    - source: salt://ambari/scripts/internal_hostname.sh
    - mode: 755

/etc/ambari-agent/conf/public_hostname.sh:
  file.managed:
    - source: salt://ambari/scripts/public_hostname.sh
    - template: jinja
    - context:
      has_public_address: {{ host.has_public_address }}
      private_address: {{ host.private_address }}
    - mode: 755

set_ambari_server_address:
  file.replace:
    - name: /etc/ambari-agent/conf/ambari-agent.ini
    - pattern: "hostname.*=.*localhost"
    - repl: "hostname={{ ambari.server_address }}"

set_public_hostname_script:
  file.replace:
    - name: /etc/ambari-agent/conf/ambari-agent.ini
    - pattern: "\\[agent\\]"
    - repl: "[agent]\npublic_hostname_script=/etc/ambari-agent/conf/public_hostname.sh"
    - unless: cat /etc/ambari-agent/conf/ambari-agent.ini | grep public_hostname_script
    - watch:
      - file: /etc/ambari-agent/conf/public_hostname.sh

set_internal_hostname_script:
  file.replace:
    - name: /etc/ambari-agent/conf/ambari-agent.ini
    - pattern: "\\[agent\\]"
    - repl: "[agent]\nhostname_script=/etc/ambari-agent/conf/internal_hostname.sh"
    - unless: cat /etc/ambari-agent/conf/ambari-agent.ini | grep -v public_hostname_script | grep hostname_script
    - watch:
      - file: /etc/ambari-agent/conf/internal_hostname.sh

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
        - file: /etc/systemd/system/ambari-agent.service

{% else %}

# Upstart case

# Avoid concurrency between SysV and Upstart
disable-ambari-agent-sysv:
  cmd.run:
    - name: chkconfig ambari-agent off
    - onlyif: chkconfig --list ambari-agent | grep on


/etc/init/ambari-agent.override:
  file.managed:
    - source: salt://ambari/upstart/ambari-agent.override

start-ambari-agent:
  service.running:
    - enable: True
    - name: ambari-agent

{% endif %}
