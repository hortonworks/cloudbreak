base:
  '*':
    - ambari.repo
    - ambari.server
    - hdp.repo
    - nodes.hosts
    - discovery.init
    - recipes.init
    - consul.init

  'roles:gateway':
    - match: grain
    - gateway.init

  'roles:kerberos_server':
    - match: grain
    - kerberos.init

  'roles:ambari_server':
    - match: grain
    - ambari.database
    - ambari.credentials
    - prometheus.server
    - grafana.repo

  'roles:knox_gateway':
    - match: grain
    - ldap.init