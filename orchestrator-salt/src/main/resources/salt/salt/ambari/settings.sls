{% if grains['init'] == 'upstart' %}
    {% set is_systemd = False %}
{% else %}
    {% set is_systemd = True %}
{% endif %}

{%- set server_address = salt['mine.get']('G@roles:ambari_server', 'network.ipaddrs', expr_form = 'compound').values()[0][0] %}
{% set is_predefined_repo = salt['pillar.get']('ambari:repo:predefined') %}
{% set is_gpl_repo_enabled = salt['pillar.get']('ambari:gpl:enabled') %}
{% set is_container_executor = salt['pillar.get']('docker:enableContainerExecutor') %}
{% set version = salt['pillar.get']('ambari:repo:version') %}
{% set ambari_database = salt['pillar.get']('ambari:database') %}
{%- set cluster_domain = salt['pillar.get']('hosts')[salt['network.interface_ip']('eth0')]['domain'] %}
{% set is_local_ldap = salt['pillar.get']('ldap:local', False) %}
{% set ldap = salt['pillar.get']('ldap') %}
{% set gateway = salt['pillar.get']('gateway') %}
{% set username = salt['pillar.get']('ambari:username') %}
{% set password = salt['pillar.get']('ambari:password') %}
{% set security_master_key = salt['pillar.get']('ambari:securityMasterKey') %}
{% if salt['pillar.get']('ldap:protocol').startswith('ldaps') %}
  {% set secure_ldap = 'true' %}
{% else %}
  {% set secure_ldap = 'false' %}
{% endif %}

{% set ambari = {} %}
{% do ambari.update({
    'is_systemd' : is_systemd,
    'server_address' : server_address,
    'is_predefined_repo' : is_predefined_repo,
    'is_gpl_repo_enabled' : is_gpl_repo_enabled,
    'version': version,
    'ambari_database': ambari_database,
    'cluster_domain': cluster_domain,
    'is_container_executor': is_container_executor,
    'is_local_ldap': is_local_ldap,
    'secure_ldap': secure_ldap,
    'ldap': ldap,
    'gateway': gateway,
    'username': username,
    'password': password,
    'security_master_key': security_master_key
}) %}