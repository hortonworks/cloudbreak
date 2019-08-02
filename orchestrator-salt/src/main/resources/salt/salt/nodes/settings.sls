# https://tedops.github.io/how-to-find-default-active-ethernet-interface.html
{% set private_address = salt['network.interface_ip'](salt['network.default_route']('inet')[0]['interface']) %}
{% set host_details = salt['pillar.get']('hosts')[private_address] %}
{% set has_public_address = host_details['public_address'] %}
{% set instance_id = host_details['instance_id'] %}
{% set instance_type = host_details['instance_type'] %}

{% set host = {} %}
{% do host.update({
    'has_public_address' : has_public_address,
    'private_address' : private_address,
    'instance_id' : instance_id,
    'instance_type' : instance_type
}) %}
