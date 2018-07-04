base:
  '*':
    - consul
    - unbound
    - java
    - metadata
{% if not salt['file.directory_exists']('/yarn-private') %}  # FIXME (BUG-92637): must be disabled for YCloud
    - nginx
{% endif %}
    - docker

  'roles:kerberos_server_master':
    - match: grain
    - kerberos.master

  'roles:kerberos_server_slave':
    - match: grain
    - kerberos.slave

  'G@roles:ambari_upgrade and G@roles:ambari_agent':
    - match: compound
    - ambari.agent-upgrade
    - smartsense.agent-upgrade

  'G@roles:ambari_upgrade and G@roles:ambari_server':
    - match: compound
    # smartsense needs to run before the Ambari server upgrade, because it needs a running server
    - smartsense.server-upgrade
    - ambari.server-upgrade

  'G@roles:ambari_upgrade and G@roles:ambari_server_standby':
    - match: compound
    # smartsense needs to run before the Ambari server upgrade, because it needs a running server
    - smartsense.server-upgrade
    - ambari.server-upgrade

  'roles:gateway':
    - match: grain
    - gateway

  'roles:smartsense_agent_update':
    - match: grain
    - smartsense.agent-update

  'roles:postgresql_server':
    - match: grain
    - postgresql.postgres-install

  'roles:ambari_server_install':
    - match: grain
    - prometheus.server
    - ambari.server

  'roles:ambari_agent_install':
    - match: grain
    - ambari.agent

  'roles:smartsense':
    - match: grain
    - smartsense

  'recipes:pre-ambari-start':
    - match: grain
    - pre-recipes.pre-ambari-start

  'roles:ambari_server':
    - match: grain
    - ambari.server-start

  'roles:ambari_server_standby':
    - match: grain
    - ambari.server-stop

  'roles:ambari_agent':
    - match: grain
    - ambari.agent-start

  'recipes:post-ambari-start':
    - match: grain
    - pre-recipes.post-ambari-start

  'G@roles:ambari_server and G@recipes:post-ambari-start':
    - match: compound
    - ambari.sync-ldap

  'recipes:pre-termination':
    - match: grain
    - pre-recipes.pre-termination

  'recipes:post-cluster-install':
    - match: grain
    - post-recipes

  'G@recipes:post and G@roles:kerberos_server_slave':
    - match: compound
    - kerberos.kprop