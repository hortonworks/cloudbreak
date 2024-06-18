{% set ldap = salt['pillar.get']('ldap') %}
{% set knox_data_root =  salt['pillar.get']('gateway:knoxDataRoot') %}
{% set knox_gateway_secret_root =  salt['pillar.get']('gateway:knoxGatewaySecurityDir') %}
{% set keystore_type = 'bcfks' if salt['pillar.get']('cluster:gov_cloud', False) == True else 'jks' %}


{% set gateway = {} %}
{% do gateway.update({
    'ldap': ldap,
    'knox_data_root': knox_data_root,
    'knox_gateway_secret_root': knox_gateway_secret_root,
    'keystore_type': keystore_type
}) %}