base:
  '*':
    - kernel.init

  'platform:OPENSTACK':
    - match: pillar
    - discovery.init

  'platform:GCP':
    - match: pillar
    - discovery.init

  'platform:AZURE_RM':
    - match: pillar
    - discovery.init

  'roles:kerberos_server':
    - match: grain
    - kerberos.server

  'platform:AWS':
    - match: pillar

  'roles:ambari_server':
    - match: grain
    - ambari.server

  'roles:ambari_agent':
    - match: grain
    - ambari.agent

  'I@platform:AWS and G@roles:smartsense':
    - match: compound
    - smartsense.init

  'I@platform:AWS and G@roles:smartsense and G@roles:ambari_server':
    - match: compound
    - smartsense.gateway-init

