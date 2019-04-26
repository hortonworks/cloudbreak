{% set server = salt['pillar.get']('sssd-ipa:server') %}
{% set principal = salt['pillar.get']('sssd-ipa:principal') %}
{% set realm = salt['pillar.get']('sssd-ipa:realm') %}

{% set ipa = {} %}
{% do ipa.update({
    'server': server,
    'principal': principal,
    'realm': realm
}) %}

