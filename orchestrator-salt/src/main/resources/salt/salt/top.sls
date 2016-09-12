base:
  '*':
    - discovery
    - users

  'platform:AWS':
    - match: pillar
    - dns

  'roles:kerberos_server':
    - match: grain
    - kerberos.server

  'roles:ambari_server':
    - match: grain
    - ambari.server

  'roles:ambari_agent':
    - match: grain
    - ambari.agent

  'G@recipes:post and G@roles:knox_gateway':
    - match: compound
    - ldap

  'I@platform:AWS and G@roles:smartsense':
    - match: compound
    - smartsense

  'recipes:pre':
    - match: grain
    - pre-recipes

  'recipes:post':
    - match: grain
    - post-recipes
    - users.add-to-group

