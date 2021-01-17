base:
  '*':
    - pkg-mgr-proxy
    - unbound
    - java
    - metadata
    - tags
    - docker
    - recipes.runner
    - telemetry
    - fluent
    - metering
    - ntp
    - postgresql.root-certs

  'G@roles:ad_member and G@os_family:RedHat':
    - match: compound
    - sssd.ad

  'G@roles:ipa_member and G@os_family:RedHat':
    - match: compound
    - sssd.ipa

  'G@roles:manager_upgrade and G@roles:manager_server':
    - match: compound
    - cloudera.manager.upgrade

  'G@roles:manager_upgrade and G@roles:manager_agent':
    - match: compound
    - cloudera.agent.upgrade

  'G@roles:manager_server':
    - postgresql
    - cloudera.csd
    - cloudera.repo
    - cloudera.manager
    - cloudera.agent
    - gateway.cm
    - nodestatus

  'G@roles:manager_agent':
    - cloudera.repo
    - cloudera.agent

  'roles:kerberized':
    - match: grain
    - kerberos.common

  'G@roles:kerberized and G@roles:manager_server':
    - match: compound
    - kerberos.cm-keytab

  'roles:postgresql_server':
    - match: grain
    - postgresql

  # The reason why we need gateway and knox is becuse the knox role is not applied if the CM template does have Knox in it, and CB injects it automatically
  'G@roles:gateway or G@roles:knox':
    - match: compound
    - gateway.knox
    - ccm
    - nginx.init

  'G@roles:idbroker':
    - match: compound
    - idbroker

  'recipes:pre-cloudera-manager-start':
    - match: grain
    - recipes.pre-cloudera-manager-start

  'roles:manager_server':
    - match: grain
    - cloudera.manager.start
    - nginx.init

  'roles:manager_agent':
    - match: grain
    - cloudera.agent.start

  'recipes:post-cloudera-manager-start':
    - match: grain
    - recipes.post-cloudera-manager-start

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

  'roles:cloudera_manager_agent_stop':
    - match: grain
    - cloudera.agent.agent-stop
