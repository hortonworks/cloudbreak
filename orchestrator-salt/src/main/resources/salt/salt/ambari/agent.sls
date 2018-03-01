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

{% if salt['pillar.get']('platform') == 'GCP' %}
/etc/environment:
  file.append:
    - text: "SPARK_CLASSPATH=${SPARK_CLASSPATH}:/usr/lib/hadoop/lib/*"
    - unless: cat /etc/environment | grep SPARK_CLASSPATH
{% endif %}

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
    - pattern: "^hostname[ ]{0,1}=.*"
    - repl: "hostname=ambari-server.{{ ambari.cluster_domain }}"

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

add_amazon2017_patch_script_agent:
  file.managed:
    - name: /tmp/amazon2017.sh
    - source: salt://ambari/scripts/amazon2017.sh
    - skip_verify: True
    - makedirs: True
    - mode: 755

run_amazon2017_sh_agent:
  cmd.run:
    - name: sh -x /tmp/amazon2017.sh 2>&1 | tee -a /var/log/amazon2017_agent_sh.log && exit ${PIPESTATUS[0]}
    - unless: ls /var/log/amazon2017_agent_sh.log
    - require:
      - file: add_amazon2017_patch_script_agent

{% if ambari.is_container_executor %}

/opt/setup_container_executor_agent.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/scripts/setup_container_executor_agent.sh
    - mode: 744

modify_container_executor_template_agent:
  cmd.run:
    - name: /opt/setup_container_executor_agent.sh

{% endif %}
