{% set ambari_server = salt['pillar.get']('ambari:server') %}
{% set platform = salt['pillar.get']('platform') %}

{% set metadata = {} %}
{% do metadata.update({
    'ambari_server' : ambari_server,
    'platform' : platform
}) %}