{% set cpuarch = salt['grains.get']('cpuarch') %}

base:
  '*':
    - prevalidation
    - pkg-mgr-proxy
    - unbound
    - java
    - metadata
    - tags
    - recipes.runner
    - telemetry
    - fluent
    - monitoring
    - logrotate
    - ntp
    - postgresql.root-certs
    - rhelrepo
    {% if salt['pillar.get']('cluster:secretEncryptionEnabled', False) == True %}
    - cdpluksvolumebackup
    {% endif %}
    - hostname
    - selinux
    {% if salt['pillar.get']('cluster:hybridEnabled', False) == True %}
    - hybridbase
    {% endif %}

  'G@roles:ad_member and G@os_family:RedHat':
    - match: compound
    - sssd.ad

  'G@roles:ipa_member and G@os_family:RedHat':
    - match: compound
    - sssd.ipa
    - faillock

  'G@roles:manager_upgrade and G@roles:manager_server':
    - match: compound
    - cloudera.manager.upgrade

  'G@roles:manager_upgrade and G@roles:manager_agent':
    - match: compound
    - cloudera.agent.upgrade

  'roles:kerberized':
    - match: grain
    - kerberos.common

  'G@roles:kerberized and G@roles:manager_server':
    - match: compound
    - kerberos.cm-keytab

  'G@roles:manager_server':
    - postgresql
    - cloudera.csd
    - cloudera.repo
    - cloudera.manager
    - cloudera.agent
    - gateway.cm
    - atlas

  'G@roles:manager_agent':
    - cloudera.repo
    - cloudera.agent

  'roles:postgresql_server':
    - match: grain
    - postgresql

  'roles:startup_mount':
    - match: grain
    - disks.service

  # The reason why we need gateway and knox is because the knox role is not applied if the CM template does have Knox in it, and CB injects it automatically
  # CB-10699 DH HA has knox nodes on non-gateway nodes, so nginx shouldn't be installed there, but gateway's have knox.
  'G@roles:gateway':
    - match: compound
    - gateway.knox
    - ccm
    - nginx.init
    - gateway.floating_ip_loadbalancer

  'G@roles:knox':
    - match: compound
    - gateway.knox
    - ccm

  'G@roles:idbroker':
    - match: compound
    - idbroker

  'recipes:pre-cloudera-manager-start':
    - match: grain
    - recipes.pre-cloudera-manager-start

  'recipes:pre-service-deployment':
    - match: grain
    - recipes.pre-service-deployment

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

  'recipes:post-service-deployment':
    - match: grain
    - recipes.post-service-deployment

  'G@roles:ad_leave and G@os_family:RedHat':
    - match: compound
    - sssd.ad-leave

  'G@roles:ipa_leave and G@os_family:RedHat':
    - match: compound
    - sssd.ipa-leave

  'roles:cloudera_manager_agent_stop':
    - match: grain
    - cloudera.agent.agent-stop

  'roles:cloudera_manager_full_stop':
    - match: grain
    - cloudera.agent.agent-stop
    - cloudera.manager.server-stop

  'roles:cloudera_manager_full_start':
    - match: grain
    - cloudera.agent.start
    - cloudera.manager.server-start
