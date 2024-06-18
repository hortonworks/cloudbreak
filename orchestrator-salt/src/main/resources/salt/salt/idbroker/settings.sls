{% set knox_data_root =  salt['pillar.get']('idbroker:knoxDataRoot') %}
{% set knox_idbroker_secret_root =  salt['pillar.get']('idbroker:knoxIdBrokerSecurityDir') %}
{% set keystore_type = 'bcfks' if salt['pillar.get']('cluster:gov_cloud', False) == True else 'jks' %}

{% set idbroker = {} %}
{% do idbroker.update({
    'knox_data_root': knox_data_root,
    'knox_idbroker_secret_root': knox_idbroker_secret_root,
    'keystore_type': keystore_type
}) %}