{% set advertise_addr = salt['network.interface_ip'](pillar['network_interface']) %}

{% set roles = salt['grains.get']('roles') %}
{% if 'ambari_server' in roles or 'ambari_server_standby' in roles %}
    {% set is_server = True %}
{% else %}
    {% set is_server = False %}
{% endif %}

{% set node_name = salt['grains.get']('nodename') %}

{% set servers = [] %}
{%- for host, host_ips in salt['mine.get']('G@roles:ambari_server or G@roles:ambari_server_standby', 'network.ipaddrs', expr_form = 'compound').items() %}
  {%- for ip, args in pillar.get('hosts', {}).items() %}
    {% if ip in host_ips %}
      {% do servers.append(ip) %}
    {% endif %}
  {%- endfor %}
{%- endfor %}
{% set bootstrap_expect =  servers|length %}

{% set consul = {} %}
{% do consul.update({
    'is_server': is_server,
    'advertise_addr': advertise_addr,
    'node_name': node_name,
    'bootstrap_expect': bootstrap_expect,
    'retry_join': servers
}) %}
