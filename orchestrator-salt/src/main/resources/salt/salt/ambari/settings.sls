{% if grains['init'] == 'upstart' %}
    {% set is_systemd = False %}
{% else %}
    {% set is_systemd = True %}
{% endif %}

{% set server_address = [] %}
{%- for host, host_ips in salt['mine.get']('G@roles:ambari_server', 'network.ipaddrs', expr_form = 'compound').items() %}
  {%- for ip, args in pillar.get('hosts', {}).items() %}
    {% if ip in host_ips %}
      {% do server_address.append(ip) %}
    {% endif %}
  {%- endfor %}
{%- endfor %}
{% set server_address = server_address[0] %}

{% set is_gpl_repo_enabled = salt['pillar.get']('ambari:gpl:enabled') %}
{% set setup_ldap_and_sso_on_api = salt['pillar.get']('ambari:setup_ldap_and_sso_on_api') %}
{% set is_container_executor = salt['pillar.get']('docker:enableContainerExecutor') %}
{% set version = salt['pillar.get']('ambari:repo:version') %}
{% set ambari_database = salt['pillar.get']('ambari:database') %}
{% set cluster_domain = salt['pillar.get']('hosts')[server_address]['domain'] %}
{% set is_local_ldap = salt['pillar.get']('ldap:local', False) %}
{% set ldap = salt['pillar.get']('ldap') %}
{% set gateway = salt['pillar.get']('gateway') %}
{% set username = salt['pillar.get']('ambari:username') %}
{% set password = salt['pillar.get']('ambari:password') %}
{% set security_master_key = salt['pillar.get']('ambari:securityMasterKey') %}
{% if salt['pillar.get']('ldap:protocol').lower().startswith('ldaps') %}
  {% set secure_ldap = 'true' %}
{% else %}
  {% set secure_ldap = 'false' %}
{% endif %}
{% set stack_type = salt['pillar.get']('ambari:repo:stack_type') %}
{% set stack_version = salt['pillar.get']('ambari:repo:stack_version') %}

{% set ambari = {} %}
{% do ambari.update({
    'is_systemd' : is_systemd,
    'server_address' : server_address,
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
    'security_master_key': security_master_key,
    'stack_type': stack_type,
    'stack_version': stack_version,
    'setup_ldap_and_sso_on_api': setup_ldap_and_sso_on_api
}) %}