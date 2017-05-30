{%- from 'ambari/settings.sls' import ambari with context %}

{% if not ambari.is_predefined_repo %}

include:
  - ambari.repo

ambari-server:
  pkg.installed:
    - require:
      - sls: ambari.repo
    - version: {{ ambari.version }}

{% else %}

lazy_view_load:
  file.append:
    - name: /etc/ambari-server/conf/ambari.properties
    - text: view.extract-after-cluster-config=true
    - unless: grep "view.extract-after-cluster-config" /etc/ambari-server/conf/ambari.properties

parallel_topology_task_execution:
  file.append:
    - name: /etc/ambari-server/conf/ambari.properties
    - text: topology.task.creation.parallel=true
    - unless: grep "topology.task.creation.parallel" /etc/ambari-server/conf/ambari.properties

disable_agent_cache_update:
  file.append:
    - name: /etc/ambari-server/conf/ambari.properties
    - text: agent.auto.cache.update=false
    - unless: grep "agent.auto.cache.update" /etc/ambari-server/conf/ambari.properties

provision_action_based_on_real_dependencies:
  file.append:
    - name: /etc/ambari-server/conf/ambari.properties
    - text: server.stage.command.execution_type=DEPENDENCY_ORDERED
    - unless: grep "server.stage.command.execution_type" /etc/ambari-server/conf/ambari.properties

{% endif %}

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

/var/lib/ambari-server/jdbc-drivers:
  cmd.run:
    - name: cp -R /opt/jdbc-drivers /var/lib/ambari-server/jdbc-drivers
    - unless: ls -1 /var/lib/ambari-server/jdbc-drivers

/opt/ambari-server/ambari-server-init.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/scripts/ambari-server-init.sh
    - template: jinja
    - context:
      ambari_database: {{ ambari.ambari_database }}
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

modify_hadoop_env_template:
  cmd.run:
    - name: /opt/javaagent.sh

add_amazon2017_patch_script_server:
  file.managed:
    - name: /tmp/amazon2017.sh
    - source: salt://ambari/scripts/amazon2017.sh
    - skip_verify: True
    - makedirs: True
    - mode: 755

run_amazon2017_sh_server:
  cmd.run:
    - name: sh -x /tmp/amazon2017.sh 2>&1 | tee -a /var/log/amazon2017_server_sh.log && exit ${PIPESTATUS[0]}
    - unless: ls /var/log/amazon2017_server_sh.log
    - require:
      - file: add_amazon2017_patch_script_server

{% if 'HDF' in salt['pillar.get']('hdp:stack:repoid') %}

/opt/ambari-server/install-hdf-mpack.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/scripts/install-hdf-mpack.sh
    - template: jinja
    - mode: 744
    - context:
      mpack: {{ salt['pillar.get']('hdp:stack:mpack') }}

install_hdf_mpack:
  cmd.run:
    - name: /opt/ambari-server/install-hdf-mpack.sh
    - shell: /bin/bash
    - unless: test -f /var/hdf_mpack_installed

{% endif %}
