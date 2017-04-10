{%- set ambari_server = salt['mine.get']('G@roles:ambari_server', 'network.ipaddrs', expr_form = 'compound').values()[0][0] %}
{% set platform = salt['pillar.get']('platform') %}

{% set metadata = {} %}
{% do metadata.update({
    'ambari_server' : ambari_server,
    'platform' : platform
}) %}