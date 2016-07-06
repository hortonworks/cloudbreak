{% if grains['init'] == 'upstart' %}
    {% set is_systemd = False %}
{% else %}
    {% set is_systemd = True %}
{% endif %}

{% set server_address = salt['pillar.get']('ambari:server') %}

{% set is_predefined_repo = salt['pillar.get']('ambari:repo:predefined') %}

{% set ambari = {} %}
{% do ambari.update({
    'is_systemd' : is_systemd,
    'server_address' : server_address,
    'is_predefined_repo' : is_predefined_repo
}) %}