base:
  '*':
    - ambari.repo
    - ambari.server
    - nodes.hosts

  'roles:kerberos_server':
    - match: grain
    - kerberos.init