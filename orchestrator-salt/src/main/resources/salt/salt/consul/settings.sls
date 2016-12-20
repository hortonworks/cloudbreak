{% set advertise_addr = salt['network.interface_ip']('eth0') %}
{% if 'ambari_server' in salt['grains.get']('roles') %}
    {% set is_server = True %}
{% else %}
    {% set is_server = False %}
{% endif %}
{% set node_name = salt['grains.get']('nodename') %}
{% set bootstrap_expect =  1 %}
{% set retry_join = [salt['pillar.get']('ambari:server')] %}

{% set consul = {} %}
{% do consul.update({
    'server': salt['pillar.get']('consul:server'),
    'is_server': is_server,
    'node_name': node_name,
    'bootstrap_expect': bootstrap_expect,
    'retry_join': retry_join
}) %}
