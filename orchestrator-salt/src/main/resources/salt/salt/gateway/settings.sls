{% set ldap = salt['pillar.get']('ldap') %}
{% set knox_data_root =  salt['pillar.get']('gateway:knoxDataRoot') %}
{% set keystore_type = 'bcfks' if salt['pillar.get']('cluster:gov_cloud', False) == True else 'jks' %}

{% set gateway = {} %}
{% do gateway.update({
    'ldap': ldap,
    'knox_data_root': knox_data_root,
    'keystore_type': keystore_type
}) %}