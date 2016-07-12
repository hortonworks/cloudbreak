base:
  '*':
    - ambari.repo
    - ambari.server
    - nodes.hosts
    - discovery.init
    - recipes.init

  'roles:kerberos_server':
    - match: grain
    - kerberos.init