{% set knox_data_root =  salt['pillar.get']('idbroker:knoxDataRoot') %}

{% set idbroker = {} %}
{% do idbroker.update({
    'knox_data_root': knox_data_root
}) %}