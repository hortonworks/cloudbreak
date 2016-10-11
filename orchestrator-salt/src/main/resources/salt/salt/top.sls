base:
  '*':
    - discovery
    - java

  'roles:kerberos_server':
    - match: grain
    - kerberos

  'roles:ambari_server':
    - match: grain
    - ambari.server

  'roles:ambari_agent':
    - match: grain
    - ambari.agent

  'roles:smartsense':
    - match: grain
    - smartsense

  'recipes:pre':
    - match: grain
    - pre-recipes

  'G@recipes:post and G@roles:knox_gateway':
    - match: compound
    - ldap

  'recipes:post':
    - match: grain
    - post-recipes
