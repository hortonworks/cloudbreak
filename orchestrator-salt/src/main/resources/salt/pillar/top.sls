base:
  'G@roles:ambari_server or G@roles:ambari_agent':
    - match: compound
    - ambari.config
    - ambari.repo
    - ambari.gpl
    - hdp.repo

  '*':
    - nodes.hosts
    - discovery.init
    - recipes.init
    - unbound.forwarders
    - datalake.init
    - docker
    - metadata.init
    - proxy.proxy
    - databus
    - cloudera-manager.csd
    - fluent
    - metering
    - mount.disk

  'G@roles:ad_member or G@roles:ad_leave':
    - match: compound
    - sssd.ad

  'G@roles:ipa_member or G@roles:ad_leave':
    - match: compound
    - sssd.ipa

  'G@roles:ipa_leave':
    - match: compound
    - sssd.ipa

  'G@roles:gateway or G@roles:knox':
    - match: compound
    - gateway.init
    - gateway.ldap
    - gateway.settings

  'roles:kerberized':
    - match: grain
    - kerberos.init

  'G@roles:kerberized and G@roles:manager_server':
    - match: compound
    - kerberos.keytab

  'roles:postgresql_server':
    - match: grain
    - postgresql.postgre

  'roles:manager_agent':
    - match: grain
    - cloudera-manager.repo
    - cloudera-manager.communication

  'roles:manager_server':
    - match: grain
{% if salt['file.file_exists']('/srv/pillar/cloudera-manager/license.sls') %}
    - cloudera-manager.license
{% endif %}
{% if salt['file.file_exists']('/srv/pillar/cloudera-manager/cme.sls') %}
    - cloudera-manager.cme
{% endif %}
    - cloudera-manager.repo
    - cloudera-manager.database
    - cloudera-manager.communication
    - cloudera-manager.autotls
    - gateway.init
    - gateway.ldap
    - gateway.settings

  'roles:ambari_server*':
    - match: grain
    - ambari.database
    - ambari.credentials
    - ambari.ldaps
    - grafana.repo
    - gateway.init
    - gateway.ldap
    - gateway.settings
    - jdbc.connectors

  'roles:knox_gateway':
    - match: grain
    - ldap.init

  'roles:smartsense_agent_update':
    - match: grain
    - smartsense

  'roles:smartsense':
    - match: grain
    - smartsense
    - smartsense.credentials
