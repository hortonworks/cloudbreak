{% set realm = salt['grains.get']('domain') %}
{% set url = salt['pillar.get']('kerberos:url') %}
{% set adminUrl = salt['pillar.get']('kerberos:adminUrl') %}

{% set servers = [] %}
{%- for host, host_ips in salt['mine.get']('G@roles:kerberos_server_master or G@roles:kerberos_server_slave', 'network.ipaddrs', expr_form = 'compound').items() %}
  {%- for ip, args in pillar.get('hosts', {}).items() %}
    {% if ip in host_ips %}
      {% do servers.append(args['fqdn']) %}
    {% endif %}
  {%- endfor %}
{%- endfor %}

{% set enable_iprop = 'false' %}
{% if servers|length > 1 %}
    {% set enable_iprop = 'true' %}
{% endif %}

{% set kerberos = {} %}
{% do kerberos.update({
    'realm': realm|upper,
    'url': url,
    'adminUrl': adminUrl,
    'kdcs': servers|join(" "),
    'enable_iprop': enable_iprop
}) %}