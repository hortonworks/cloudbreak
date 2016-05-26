base:
  '*':
    - ambari.repo
    - ambari.server
    - nodes.hosts
    - discovery.init

  'roles:kerberos_server':
    - match: grain
    - kerberos.init