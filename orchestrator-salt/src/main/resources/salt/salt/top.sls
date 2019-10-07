base:
  '*':
    - pkg-mgr-proxy
    - unbound
    - java
    - metadata
    - docker
    - recipes.runner
    - fluent
    - metering
    - ntp

  'G@roles:ad_member and G@os_family:RedHat':
    - match: compound
    - sssd.ad

  'G@roles:ipa_member and G@os_family:RedHat':
    - match: compound
    - sssd.ipa

  'G@roles:manager_server':
    - postgresql
    - cloudera.csd
    - cloudera.repo
    - cloudera.manager
    - cloudera.agent

  'G@roles:manager_agent':
    - cloudera.repo
    - cloudera.agent

  'roles:kerberized':
    - match: grain
    - kerberos.common

  'G@roles:kerberized and G@roles:manager_server':
    - match: compound
    - kerberos.cm-keytab

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

  'roles:smartsense_agent_update':
    - match: grain
    - smartsense.agent-update

  'roles:postgresql_server':
    - match: grain
    - postgresql

  'roles:ambari_server_install':
    - match: grain
    - ambari.server
    - jdbc.connectors

  'roles:ambari_agent_install':
    - match: grain
    - ambari.agent

  'G@roles:gateway and G@roles:ambari*':
    - match: compound
    - gateway.ambari

  'G@roles:gateway and G@roles:manager*':
    - match: compound
    - gateway.cm

  'roles:smartsense':
    - match: grain
    - smartsense

  'recipes:pre-cloudera-manager-start':
    - match: grain
    - recipes.pre-cloudera-manager-start

  'roles:ambari_server':
    - match: grain
    - ambari.server-start
{% if not salt['file.directory_exists']('/yarn-private') %}  # FIXME (BUG-92637): must be disabled for YCloud
    - nginx.init
{% endif %}

  'roles:manager_server':
    - match: grain
    - cloudera.manager.start
    - nginx.init

  'roles:manager_agent':
    - match: grain
    - cloudera.agent.start

  'roles:ambari_server_standby':
    - match: grain
    - ambari.server-stop

  'roles:ambari_agent':
    - match: grain
    - ambari.agent-start

  'recipes:post-cloudera-manager-start':
    - match: grain
    - recipes.post-cloudera-manager-start

  'G@roles:ambari_server and G@recipes:post-cloudera-manager-start':
    - match: compound
    - ambari.sync-ldap

  'recipes:pre-termination':
    - match: grain
    - recipes.pre-termination

  'recipes:post-cluster-install':
    - match: grain
    - recipes.post-cluster-install

  'G@roles:ad_leave and G@os_family:RedHat':
    - match: compound
    - sssd.ad-leave

  'G@roles:ipa_leave and G@os_family:RedHat':
    - match: compound
    - sssd.ipa-leave

  'roles:ambari_single_master_repair_stop':
    - match: grain
    - ambari.server-stop
    - ambari.agent-stop

  'roles:cloudera_manager_agent_stop':
    - match: grain
    - cloudera.agent.agent-stop
