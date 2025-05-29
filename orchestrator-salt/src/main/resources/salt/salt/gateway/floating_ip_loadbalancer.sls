{%- set os = salt['grains.get']('os') %}
{%- if (salt['pillar.get']('platform') == 'AZURE' and salt['pillar.get']('gateway:loadbalancers:floatingIpEnabled', False))
  or (salt['pillar.get']('platform') == 'GCP') and salt['pillar.get']('gateway:loadbalancers:frontends') is defined and salt['pillar.get']('gateway:loadbalancers:frontends') | length > 0 %}
{%- if os == "RedHat" %}
create_loopback_service_unit:
  file.managed:
    - name: /etc/systemd/system/loopback.service
    - source: salt://gateway/services/loopback.service.j2
    - template: jinja
    - makedirs: True
    - user: root
    - group: root
    - mode: 664

start_and_enable_loopback_service:
  service.running:
    - name: loopback
    - enable: True
    - unless: test $(/sbin/ip addr show lo | grep -c inet) -gt 1
{%- else %}
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
{%- for lb in salt['pillar.get']('gateway:loadbalancers:frontends') %}
  {%- if lb['type'] == 'GATEWAY_PRIVATE' or lb['type'] == 'PRIVATE' %}
      - {{ lb['ip'] }}/32
  {%- endif %}
{%- endfor %}
{%- endif %}
{%- endif %}
