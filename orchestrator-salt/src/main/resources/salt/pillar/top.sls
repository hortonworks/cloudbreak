base:
  '*':
    - consul.init
    - ambari.repo

  'roles:kerberos_server':
    - match: grain
    - kerberos.init