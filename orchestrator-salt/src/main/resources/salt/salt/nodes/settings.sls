{% set has_public_address = salt['pillar.get']('hosts')[salt['network.interface_ip']('eth0')]['public_address'] %}
{% set private_address = salt['network.interface_ip']('eth0') %}

{% set host = {} %}
{% do host.update({
    'has_public_address' : has_public_address,
    'private_address' : private_address
}) %}
