{% if grains['init'] == 'upstart' %}
    {% set is_systemd = False %}
{% else %}
    {% set is_systemd = True %}
{% endif %}
{% set advertise_addr = salt['grains.get']('consul:advertise_addr') %}
{% if 'consul_server' in salt['grains.get']('roles') %}
    {% set is_server = True %}
{% else %}
    {% set is_server = False %}
{% endif %}
{% set node_name = salt['grains.get']('nodename') %}
{% set recursors = salt['grains.get']('consul:recursors', '8.8.8.8') %}
{% set bootstrap_expect = salt['pillar.get']('consul:bootstrap_expect', 1) %}
{% set retry_join = salt['pillar.get']('consul:retry_join') %}

{% set consul = {} %}
{% do consul.update({
    'advertise_addr': advertise_addr,
    'is_server': is_server,
    'node_name': node_name,
    'recursors': recursors,
    'bootstrap_expect': bootstrap_expect,
    'retry_join': retry_join,
    'is_systemd' : is_systemd
}) %}