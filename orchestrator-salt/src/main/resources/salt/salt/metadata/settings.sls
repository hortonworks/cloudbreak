{% set server_address = [] %}
{%- for amb_host, amb_host_ips in salt['mine.get']('G@roles:ambari_server', 'network.ipaddrs', expr_form = 'compound').items() %}
  {%- for ip, args in pillar.get('hosts', {}).items() %}
    {% if ip in amb_host_ips %}
      {% do server_address.append(ip) %}
    {% endif %}
  {%- endfor %}
{%- endfor %}
{% set server_address = server_address[0] %}
{% set platform = salt['pillar.get']('platform') %}
{% set clusterName = salt['pillar.get']('cluster:name') %}

{% set metadata = {} %}
{% do metadata.update({
    'ambari_server' : server_address,
    'platform' : platform,
    'clusterName' : clusterName
}) %}
