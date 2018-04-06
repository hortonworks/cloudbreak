{%- from 'ambari/settings.sls' import ambari with context %}

include:
  - ambari.server-install
  - ambari.setup-sso
{% if pillar['proxy'] is defined and pillar['proxy']['host'] is defined and pillar['proxy']['port'] is defined and pillar['proxy']['protocol'] is defined %}
  - ambari.proxy
{% endif %}

lazy_view_load:
  file.append:
    - name: /etc/ambari-server/conf/ambari.properties
    - text: view.extract-after-cluster-config=true
    - unless: grep "view.extract-after-cluster-config" /etc/ambari-server/conf/ambari.properties

provision_action_based_on_real_dependencies:
  file.append:
    - name: /etc/ambari-server/conf/ambari.properties
    - text: server.stage.command.execution_type=DEPENDENCY_ORDERED
    - unless: grep "server.stage.command.execution_type" /etc/ambari-server/conf/ambari.properties

{% if ambari.ambari_database.vendor == 'mysql' %}
install-mariadb:
  pkg.installed:
    - pkgs:
      - mariadb
{% endif %}

/opt/hadoop-classpath.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/scripts/hadoop-classpath.sh
    - mode: 744

extend_hadoop_classpath:
  cmd.run:
    - name: /opt/hadoop-classpath.sh

/opt/ambari-server/ambari-server-init.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/scripts/ambari-server-init.sh
    - template: jinja
    - context:
      ambari_database: {{ ambari.ambari_database }}
      security_master_key: {{ ambari.security_master_key }}
      is_gpl_repo_enabled: {{ ambari.is_gpl_repo_enabled }}
    - mode: 744

set_install_timeout:
  file.replace:
    - name: /etc/ambari-server/conf/ambari.properties
    - pattern: "agent.package.install.task.timeout=1800"
    - repl: "agent.package.install.task.timeout=3600"

/opt/javaagent.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/scripts/javaagent.sh
    - mode: 744

#modify_hadoop_env_template:
#  cmd.run:
#    - name: /opt/javaagent.sh

{% if ambari.is_container_executor %}

/opt/setup_container_executor_server.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/scripts/setup_container_executor_server.sh
    - mode: 744

modify_container_executor_template_server:
  cmd.run:
    - name: /opt/setup_container_executor_server.sh

{% endif %}

add_amazon2017_patch_script_server:
  file.managed:
    - name: /opt/salt/amazon2017.sh
    - source: salt://ambari/scripts/amazon2017.sh
    - skip_verify: True
    - makedirs: True
    - mode: 755

run_amazon2017_sh_server:
  cmd.run:
    - name: sh -x /opt/salt/amazon2017.sh 2>&1 | tee -a /var/log/amazon2017_server_sh.log && exit ${PIPESTATUS[0]}
    - unless: ls /var/log/amazon2017_server_sh.log
    - require:
      - file: add_amazon2017_patch_script_server

stop-service-registration:
  service.dead:
    - enable: False
    - name: service-registration

{% if salt['pillar.get']('hdp:stack:mpack') %}

/opt/ambari-server/install-mpack.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/scripts/install-mpack.sh
    - template: jinja
    - mode: 744
    - context:
      mpack: {{ salt['pillar.get']('hdp:stack:mpack') }}

install_mpack:
  cmd.run:
    - name: /opt/ambari-server/install-mpack.sh
    - shell: /bin/bash
    - unless: test -f /var/mpack_installed

{% endif %}

{% if not ambari.is_local_ldap %}

{% if ambari.ldap.adminGroup is defined and ambari.ldap.adminGroup is not none %}
set_ambari_administrators:
  file.append:
    - name: /etc/ambari-server/conf/ambari.properties
    - text: authorization.ldap.adminGroupMappingRules={{ ambari.ldap.adminGroup }}
    - unless: grep "authorization.ldap.adminGroupMappingRules" /etc/ambari-server/conf/ambari.properties
{% endif %}

/opt/ambari-server/setup-ldap.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/scripts/setup-ldap.sh
    - template: jinja
    - context:
      ldap: {{ ambari.ldap }}
      ambari: {{ ambari }}
    - mode: 744

setup_ldap:
  cmd.run:
    - name: /opt/ambari-server/setup-ldap.sh
    - shell: /bin/bash
    - unless: test -f /var/ldap_setup_success

{% endif %}