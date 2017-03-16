{% set master_key = salt['pillar.get']('kerberos:masterKey') %}
{% set realm = salt['grains.get']('domain') %}
{% set password = salt['pillar.get']('kerberos:password') %}
{% set user = salt['pillar.get']('kerberos:user') %}
{% set url = salt['pillar.get']('kerberos:url') %}
{% set clusterUser = salt['pillar.get']('kerberos:clusterUser') %}
{% set clusterPassword = salt['pillar.get']('kerberos:clusterPassword') %}

{% set servers = [] %}
{%- set ipList = salt['mine.get']('G@roles:kerberos_server_master or G@roles:kerberos_server_slave', 'network.ipaddrs', expr_form = 'compound').values() %}
{% for ips in ipList %}
    {% do servers.append(salt['pillar.get']('hosts')[ips[0]]['fqdn']) %}
{% endfor %}

{% set enable_iprop = 'false' %}
{% if servers|length > 1 %}
    {% set enable_iprop = 'true' %}
{% endif %}

{% set kerberos = {} %}
{% do kerberos.update({
    'master_key': master_key,
    'realm': realm|upper,
    'password': password,
    'user': user,
    'url': url,
    'kdcs': servers|join(" "),
    'enable_iprop': enable_iprop,
    'clusterUser': clusterUser,
    'clusterPassword': clusterPassword
}) %}