{% macro include_if_exists(directory, file='init') %}
{%- if salt['file.file_exists']('/srv/pillar/' ~ directory ~ '/' ~ file ~ '.sls') %}
    - {{ directory }}.{{ file }}
{%- endif %}
{% endmacro %}

base:
  '*':
{{ include_if_exists('postgresql', 'rotation') }}
{{ include_if_exists('postgresql', 'user') }}
{{ include_if_exists('nodes', 'hosts') }}
{{ include_if_exists('nodes', 'hostattrs') }}
{{ include_if_exists('discovery') }}
{{ include_if_exists('recipes') }}
{{ include_if_exists('unbound', 'forwarders') }}
{{ include_if_exists('unbound', 'elimination') }}
    - docker
{{ include_if_exists('metadata') }}
    - tags
    - proxy.proxy
    - telemetry.init
    - databus
{{ include_if_exists('cloudera-manager', 'csd') }}
{{ include_if_exists('cloudera-manager', 'settings') }}
    - fluent
    - monitoring.init
{{ include_if_exists('mount', 'disk') }}
{{ include_if_exists('postgresql', 'root-certs') }}
{{ include_if_exists('java') }}
{{ include_if_exists('paywall') }}
    - cdpluksvolumebackup

  'G@roles:ad_member or G@roles:ad_leave':
    - match: compound
    - sssd.ad

  'G@roles:ipa_member or G@roles:ipa_leave':
    - match: compound
    - sssd.ipa

  'G@roles:gateway or G@roles:knox':
    - match: compound
    - gateway.init
{{ include_if_exists('gateway', 'ldap') }}
    - gateway.settings

  'roles:kerberized':
    - match: grain
    - kerberos.init

  'G@roles:kerberized and G@roles:manager_server and G@roles:ipa_member':
    - match: compound
    - kerberos.keytab

  'roles:postgresql_server':
    - match: grain
    - postgresql.postgre

  'roles:manager_agent':
    - match: grain
    - cloudera-manager.repo
{{ include_if_exists('cloudera-manager', 'repo-prepare') }}
    - cloudera-manager.communication

  'roles:manager_server':
    - match: grain
{{ include_if_exists('cloudera-manager', 'license') }}
{{ include_if_exists('cloudera-manager', 'cme') }}
    - cloudera-manager.repo
{{ include_if_exists('cloudera-manager', 'repo-prepare') }}
    - cloudera-manager.database
    - cloudera-manager.communication
{{ include_if_exists('cloudera-manager', 'autotls') }}
    - gateway.init
{{ include_if_exists('gateway', 'ldap') }}
    - gateway.settings
    - postgresql.disaster_recovery
    - postgresql.upgrade
    - atlas.check_atlas_updated
{{ include_if_exists('postgresql', 'backup_restore_config') }}

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

  'roles:namenode':
    - match: grain
{{ include_if_exists('gateway', 'ldap') }}
