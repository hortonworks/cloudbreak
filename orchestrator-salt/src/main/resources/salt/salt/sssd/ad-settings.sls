{% set minion_id = grains['id'] %}
{% set server_hostname = minion_id[:5] + salt['hashutil.sha256_digest'](minion_id[5:])[:10] %}
{% set domain_component = 'DC=' ~ salt['pillar.get']('sssd-ad:domain') | replace('.', ',DC=') %}

{% set ad = {} %}
{% do ad.update({
    'server_hostname': server_hostname,
    'domain_component': domain_component
}) %}

