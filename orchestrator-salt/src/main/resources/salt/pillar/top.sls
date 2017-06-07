base:
  '*':
    - ambari.repo
    - hdp.repo
    - nodes.hosts
    - discovery.init
    - recipes.init
    - unbound.forwarders
    - datalake.init
    - docker

  'roles:gateway':
    - match: grain
    - gateway.init
    - gateway.ldap

  'roles:kerberos_server_master':
    - match: grain
    - kerberos.init

  'roles:kerberos_server_slave':
    - match: grain
    - kerberos.init

  'roles:ambari_server':
    - match: grain
    - ambari.database
    - ambari.credentials
    - prometheus.server
    - grafana.repo

  'roles:ambari_server_standby':
    - match: grain
    - ambari.database
    - ambari.credentials
    - prometheus.server
    - grafana.repo

  'roles:knox_gateway':
    - match: grain
    - ldap.init