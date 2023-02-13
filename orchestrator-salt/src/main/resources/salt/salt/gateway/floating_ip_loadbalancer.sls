{% if salt['pillar.get']('platform') == 'AZURE' and salt['pillar.get']('gateway:loadbalancers:floatingIpEnabled', False) %}
rename_original_ifcfg_lo:
  file.rename:
    - makedirs: True
    - name: /etc/sysconfig/network-scripts/ifcfg-lo.orig
    - source: /etc/sysconfig/network-scripts/ifcfg-lo
    - unless: test -f /etc/sysconfig/network-scripts/ifcfg-lo.orig

lo:
  network.managed:
    - name: lo
    - type: eth
    - onboot: yes
    - userctl: no
    - ipv6_autoconf: no
    - enable_ipv6: false
    - peerdns: no
    - scope: global
    - ipaddrs:
{% for lb in salt['pillar.get']('gateway:loadbalancers:frontends') %}
  {%- if lb['type'] == 'GATEWAY_PRIVATE' or lb['type'] == 'PRIVATE' %}
      - {{ lb['ip'] }}/32
  {%- endif %}
{% endfor %}
{% endif %}
