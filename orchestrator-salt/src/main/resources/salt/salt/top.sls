base:
  '*':
    - kernel.init
    - users.init

  'platform:OPENSTACK':
    - match: pillar
    - discovery.init

  'platform:GCP':
    - match: pillar
    - discovery.init

  'platform:AZURE_RM':
    - match: pillar
    - discovery.init

  'platform:AWS':
    - match: pillar
    - discovery.init
    - dns.init

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
    - ldap.init

  'I@platform:AWS and G@roles:smartsense':
    - match: compound
    - smartsense.init

  'recipes:pre':
    - match: grain
    - pre-recipes.init

  'recipes:post':
    - match: grain
    - post-recipes.init
    - users.add-to-group

