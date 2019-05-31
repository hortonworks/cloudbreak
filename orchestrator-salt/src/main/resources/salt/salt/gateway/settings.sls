{% if grains['init'] == 'upstart' %}
    {% set is_systemd = False %}
{% else %}
    {% set is_systemd = True %}
{% endif %}
{% set is_local_ldap = salt['pillar.get']('ldap:local', False) %}

{% set vdf_url = salt['pillar.get']('hdp:stack:vdf-url') %}
{% set os_family = salt['grains.get']('os_family') %}
{% set ldap = salt['pillar.get']('ldap') %}
{% set knox_data_root =  salt['pillar.get']('gateway:knoxDataRoot') %}

{% set gateway = {} %}
{% do gateway.update({
    'is_systemd' : is_systemd,
    'is_local_ldap' : is_local_ldap,
    'os_family' : os_family,
    'vdf_url' : vdf_url,
    'ldap': ldap,
    'knox_data_root': knox_data_root
}) %}