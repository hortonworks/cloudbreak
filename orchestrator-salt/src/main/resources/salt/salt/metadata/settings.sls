{% set server_address = [] %}
{%- for host, host_ips in salt['mine.get']('G@roles:manager_server', 'network.ipaddrs', expr_form = 'compound').items() %}
  {%- for ip, args in pillar.get('hosts', {}).items() %}
    {% if ip in host_ips %}
      {% do server_address.append(ip) %}
    {% endif %}
  {%- endfor %}
{%- endfor %}
{% set server_address = server_address[0] %}
{% set platform = salt['pillar.get']('platform') %}
{% set clusterName = salt['pillar.get']('cluster:name') %}
{% set cluster_domain = salt['pillar.get']('hosts')[server_address]['domain'] %}
{% set cluster_in_childenvironment = salt['pillar.get']('cluster:deployedInChildEnvironment') %}

{% set metadata = {} %}
{% do metadata.update({
    'cluster_domain' : cluster_domain,
    'server_address' : server_address,
    'platform' : platform,
    'clusterName' : clusterName,
    'cluster_in_childenvironment' : cluster_in_childenvironment
}) %}
