base:
  '*':
    - consul
    - unbound
    - java
    - metadata
    - nginx
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

  'G@roles:ambari_server and not G@roles:smartsense':
    - match: compound
    - prometheus.server
    - ambari.server
    - pre-recipes.pre-ambari-start
    - ambari.server-start

  'G@roles:ambari_server and G@roles:smartsense':
    - match: compound
    - prometheus.server
    - ambari.server
    - pre-recipes.pre-ambari-start
    - smartsense
    - ambari.server-start

  'roles:ambari_server_standby':
    - match: grain
    - prometheus.server
    - ambari.server
    - ambari.server-stop

  'roles:ambari_agent':
    - match: grain
    - ambari.agent
    - pre-recipes.pre-ambari-start
    - ambari.agent-start

  'recipes:post-ambari-start':
    - match: grain
    - pre-recipes.post-ambari-start

  'recipes:post-cluster-install':
    - match: grain
    - post-recipes

  'G@recipes:post and G@roles:kerberos_server_slave':
    - match: compound
    - kerberos.kprop