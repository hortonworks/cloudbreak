# https://tedops.github.io/how-to-find-default-active-ethernet-interface.html
network_interface: {{ salt['network.default_route']('inet')[0]['interface'] }}
