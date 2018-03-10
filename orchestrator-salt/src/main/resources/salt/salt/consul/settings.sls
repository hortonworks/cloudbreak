{% set advertise_addr = salt['network.interface_ip'](pillar['network_interface']) %}

{% set roles = salt['grains.get']('roles') %}
{% if 'ambari_server' in roles or 'ambari_server_standby' in roles %}
    {% set is_server = True %}
{% else %}
    {% set is_server = False %}
{% endif %}

{% set node_name = salt['grains.get']('nodename') %}

{%- set ipList = salt['mine.get']('G@roles:ambari_server or G@roles:ambari_server_standby', 'network.ipaddrs', expr_form = 'compound').values() %}
{% set bootstrap_expect =  ipList|length %}
{% set servers = [] %}
{% for ips in ipList %}
    {% do servers.append(ips[0]) %}
{% endfor %}

{% set consul = {} %}
{% do consul.update({
    'is_server': is_server,
    'node_name': node_name,
    'bootstrap_expect': bootstrap_expect,
    'retry_join': servers
}) %}
