{%- set os = salt['grains.get']('os') %}
{%- set osMajorRelease = salt['grains.get']('osmajorrelease') | int %}
{%- if (salt['pillar.get']('platform') == 'AZURE' and salt['pillar.get']('gateway:loadbalancers:floatingIpEnabled', False))
  or (salt['pillar.get']('platform') == 'GCP') and salt['pillar.get']('gateway:loadbalancers:frontends') is defined and salt['pillar.get']('gateway:loadbalancers:frontends') | length > 0 %}
{%- if os == "RedHat" %}
{%- if osMajorRelease == 8 %}
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

{%- else %} # osMajorRelease == 8

{%- set lo_nm_config = '/etc/NetworkManager/system-connections/lo.nmconnection' %}

manage_loopback_by_network_manager:
  cmd.run:
    - name: nmcli con add connection.id lo connection.type loopback connection.interface-name lo connection.autoconnect yes
    - failhard: True
    - unless: test -f {{ lo_nm_config }}

{%- for lb in salt['pillar.get']('gateway:loadbalancers:frontends') %}
  {%- if lb['type'] == 'GATEWAY_PRIVATE' or lb['type'] == 'PRIVATE' or salt['pillar.get']('platform') == 'GCP' %}
add_{{ lb['ip'] | replace(".", "_") }}_to_lo:
  cmd.run:
    - name: nmcli connection modify lo +ipv4.addresses {{ lb['ip'] }}/32
    - unless: grep -q {{ lb['ip'] }} {{ lo_nm_config }}
    - require:
        - cmd: manage_loopback_by_network_manager
  {%- endif %}
{% endfor %}

refresh_lo:
  cmd.run:
    - name: nmcli con up lo
    - failhard: True
    - onchanges:
{%- for lb in salt['pillar.get']('gateway:loadbalancers:frontends') %}
  {%- if lb['type'] == 'GATEWAY_PRIVATE' or lb['type'] == 'PRIVATE' or salt['pillar.get']('platform') == 'GCP' %}
      - cmd: add_{{ lb['ip'] | replace(".", "_") }}_to_lo
  {%- endif %}
{%- endfor %}

{%- endif %} # osMajorRelease == 8
{%- else %} # os == "RedHat"
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
{%- endif %} # os == "RedHat"
{%- endif %} # Azure with floating IP OR GCP AND LB has IPs
