base:

  'platform:GCP':
     - match: pillar
     - discovery.init

  'platform:AZURE_RM':
     - match: pillar
     - discovery.init

  'roles:ambari_server':
    - match: grain
    - ambari.server

  'roles:ambari_agent':
    - match: grain
    - ambari.agent

  'roles:kerberos_server':
    - match: grain
    - kerberos.server