# https://tedops.github.io/how-to-find-default-active-ethernet-interface.html
{% set has_public_address = salt['pillar.get']('hosts')[salt['network.interface_ip'](salt['network.default_route']('inet')[0]['interface'])]['public_address'] %}
{% set private_address = salt['network.interface_ip'](salt['network.default_route']('inet')[0]['interface']) %}

{% set host = {} %}
{% do host.update({
    'has_public_address' : has_public_address,
    'private_address' : private_address
}) %}
