{% if grains['init'] == 'upstart' %}
    {% set is_systemd = False %}
{% else %}
    {% set is_systemd = True %}
{% endif %}

{% set server_address = salt['pillar.get']('ambari:server') %}

{% set ambari = {} %}
{% do ambari.update({
    'is_systemd' : is_systemd,
    'server_address' : server_address
}) %}