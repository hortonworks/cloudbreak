{% set server = salt['pillar.get']('sssd-ipa:server') %}
{% set principal = salt['pillar.get']('sssd-ipa:principal') %}
{% set realm = salt['pillar.get']('sssd-ipa:realm') %}
{% set cluster_name = salt['pillar.get']('cluster:name') %}
{% set subdomain = salt['pillar.get']('sssd-ipa:subdomain') %}

{% set ipa = {} %}
{% do ipa.update({
    'server': server,
    'principal': principal,
    'realm': realm,
    'cluster_name': cluster_name
}) %}
{% if subdomain is defined and subdomain|length %}
{% do ipa.update({
    'subdomain': '.' + subdomain
}) %}
{% endif %}
