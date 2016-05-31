{% set has_public_address = salt['pillar.get']('hosts')[salt['grains.get']('consul:advertise_addr')]['public_address'] %}
{% set private_address = salt['grains.get']('consul:advertise_addr') %}

{% set host = {} %}
{% do host.update({
    'has_public_address' : has_public_address,
    'private_address' : private_address
}) %}
