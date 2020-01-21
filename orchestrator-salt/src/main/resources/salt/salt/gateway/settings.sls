{% set ldap = salt['pillar.get']('ldap') %}
{% set knox_data_root =  salt['pillar.get']('gateway:knoxDataRoot') %}

{% set gateway = {} %}
{% do gateway.update({
    'ldap': ldap,
    'knox_data_root': knox_data_root
}) %}