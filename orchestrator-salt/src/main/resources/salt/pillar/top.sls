base:
  '*':
    - ambari.repo
    - ambari.server

  'roles:kerberos_server':
    - match: grain
    - kerberos.init