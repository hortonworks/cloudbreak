base:
  '*':
    - nodes.hosts
{%- if salt['file.file_exists']('/srv/pillar/nodes/hostattrs.sls') %}
    - nodes.hostattrs
{%- endif %}
    - discovery.init
{%- if salt['file.file_exists']('/srv/pillar/recipes/init.sls') %}
    - recipes.init
{%- endif %}
{%- if salt['file.file_exists']('/srv/pillar/unbound/forwarders.sls') %}
    - unbound.forwarders
{%- endif %}
    - unbound.elimination
    - docker
    - metadata.init
    - tags
    - proxy.proxy
    - telemetry.init
    - databus
    - nodestatus
{%- if salt['file.file_exists']('/srv/pillar/cloudera-manager/csd.sls') %}
    - cloudera-manager.csd
{%- endif %}
    - cloudera-manager.settings
    - fluent
    - metering
    - monitoring.init
{%- if salt['file.file_exists']('/srv/pillar/mount/disk.sls') %}
    - mount.disk
{%- endif %}
{%- if salt['file.file_exists']('/srv/pillar/postgresql/root-certs.sls') %}
    - postgresql.root-certs
{%- endif %}

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
{%- if salt['file.file_exists']('/srv/pillar/gateway/ldap.sls') %}
    - gateway.ldap
{%- endif %}
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
{%- if salt['file.file_exists']('/srv/pillar/cloudera-manager/license.sls') %}
    - cloudera-manager.license
{%- endif %}
{%- if salt['file.file_exists']('/srv/pillar/cloudera-manager/cme.sls') %}
    - cloudera-manager.cme
{%- endif %}
    - cloudera-manager.repo
    - cloudera-manager.database
    - cloudera-manager.communication
{%- if salt['file.file_exists']('/srv/pillar/cloudera-manager/autotls.sls') %}
    - cloudera-manager.autotls
{%- endif %}
    - gateway.init
{%- if salt['file.file_exists']('/srv/pillar/gateway/ldap.sls') %}
    - gateway.ldap
{%- endif %}
    - gateway.settings
    - postgresql.disaster_recovery
    - atlas.check_atlas_updated

  'roles:knox_gateway':
    - match: grain
    - ldap.init

  'roles:idbroker':
    - match: grain
    - idbroker.init
    - idbroker.settings

  'roles:smartsense_agent_update':
    - match: grain
    - smartsense

  'roles:smartsense':
    - match: grain
    - smartsense
    - smartsense.credentials

  'roles:startup_mount':
    - match: grain
    - mount.startup
