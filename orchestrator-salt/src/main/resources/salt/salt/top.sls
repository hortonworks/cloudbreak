base:
  '*':
    - consul
    - unbound
    - java
    - metadata

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

  'roles:ambari_server':
    - match: grain
    - prometheus.server
    - ambari.server
    - ambari.server-start

  'roles:ambari_server_standby':
    - match: grain
    - prometheus.server
    - ambari.server
    - ambari.server-stop

  'roles:ambari_agent':
    - match: grain
    - ambari.agent

  'roles:smartsense':
    - match: grain
    - smartsense

  'recipes:pre':
    - match: grain
    - pre-recipes

  'recipes:post':
    - match: grain
    - post-recipes

  'G@recipes:post and G@roles:kerberos_server_slave':
    - match: compound
    - kerberos.kprop