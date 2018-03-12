base:
  '*':
    - ambari.repo
    - ambari.gpl
    - hdp.repo
    - network
    - nodes.hosts
    - discovery.init
    - recipes.init
    - unbound.forwarders
    - datalake.init
    - docker
    - metadata.init

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

  'roles:postgresql_server':
    - match: grain
    - postgresql.postgre

  'roles:ambari_server':
    - match: grain
    - ambari.database
    - ambari.credentials
    - prometheus.server
    - grafana.repo
    - gateway.init
    - gateway.ldap

  'roles:ambari_server_standby':
    - match: grain
    - ambari.database
    - ambari.credentials
    - prometheus.server
    - grafana.repo
    - gateway.init
    - gateway.ldap

  'roles:knox_gateway':
    - match: grain
    - ldap.init

  'roles:smartsense':
    - match: grain
    - smartsense.credentials