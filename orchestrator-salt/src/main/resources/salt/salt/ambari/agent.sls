{%- from 'ambari/settings.sls' import ambari with context %}
{%- from 'nodes/settings.sls' import host with context %}
{%- from 'metadata/settings.sls' import metadata with context %}

include:
  - ambari.repo

ambari-agent:
  pkg.installed:
    - require:
      - sls: ambari.repo
{% if grains['os_family'] == 'Suse' or grains['os_family'] == 'Debian' %}
    - skip_verify: True
{% endif %}

parallel_task_execution:
  file.replace:
    - name: /etc/ambari-agent/conf/ambari-agent.ini
    - pattern: parallel_execution=0
    - repl: parallel_execution=1

{% if salt['pillar.get']('platform') == 'GCP' %}
/etc/environment:
  file.append:
    - text: "SPARK_CLASSPATH=${SPARK_CLASSPATH}:/usr/lib/hadoop/lib/*"
    - unless: cat /etc/environment | grep SPARK_CLASSPATH
{% endif %}

set_server_address:
  file.replace:
    - name: /etc/ambari-agent/conf/ambari-agent.ini
    - pattern: "^hostname[ ]{0,1}=.*"
    - repl: "hostname=server.{{ metadata.cluster_domain }}"

/etc/ambari-agent/conf/internal_hostname.sh:
  file.managed:
    - source: salt://ambari/scripts/internal_hostname.sh
    - mode: 755

{% if not 'YARN' in salt['pillar.get']('platform') %}

/etc/ambari-agent/conf/public_hostname.sh:
  file.managed:
    - source: salt://ambari/scripts/public_hostname.sh
    - template: jinja
    - context:
      has_public_address: {{ host.has_public_address }}
      private_address: {{ host.private_address }}
    - mode: 755

set_public_hostname_script:
  file.replace:
    - name: /etc/ambari-agent/conf/ambari-agent.ini
    - pattern: "\\[agent\\]"
    - repl: "[agent]\npublic_hostname_script=/etc/ambari-agent/conf/public_hostname.sh"
    - unless: cat /etc/ambari-agent/conf/ambari-agent.ini | grep public_hostname_script
    - watch:
      - file: /etc/ambari-agent/conf/public_hostname.sh

{% endif %}

set_internal_hostname_script:
  file.replace:
    - name: /etc/ambari-agent/conf/ambari-agent.ini
    - pattern: "\\[agent\\]"
    - repl: "[agent]\nhostname_script=/etc/ambari-agent/conf/internal_hostname.sh"
    - unless: cat /etc/ambari-agent/conf/ambari-agent.ini | grep -v public_hostname_script | grep hostname_script
    - watch:
      - file: /etc/ambari-agent/conf/internal_hostname.sh

set_tlsv1_2:
  file.replace:
    - name: /etc/ambari-agent/conf/ambari-agent.ini
    - pattern: "\\[security\\]"
    - repl: "[security]\nforce_https_protocol=PROTOCOL_TLSv1_2"
    - unless: cat /etc/ambari-agent/conf/ambari-agent.ini | grep force_https_protocol

add_amazon-osfamily_patch_script_agent:
  file.managed:
    - name: /opt/salt/amazon-osfamily.sh
    - source: salt://ambari/scripts/amazon-osfamily.sh
    - skip_verify: True
    - makedirs: True
    - mode: 755

run_amazon-osfamily_sh_agent:
  cmd.run:
    - name: sh -x /opt/salt/amazon-osfamily.sh 2>&1 | tee -a /var/log/amazon-osfamily_agent_sh.log && exit ${PIPESTATUS[0]}
    - unless: ls /var/log/amazon-osfamily_agent_sh.log
    - require:
      - file: add_amazon-osfamily_patch_script_agent

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
