{%- set os = salt['grains.get']('os') %}
{%- set osMajorRelease = salt['grains.get']('osmajorrelease') | int %}
{%- set platform = salt['pillar.get']('platform') %}
{%- set lbEnabled = salt['pillar.get']('freeipa:loadBalancer:enabled', False) %}
{%- if lbEnabled and platform == 'GCP' and os == "RedHat" %}
{%- if osMajorRelease == 8 %}
/opt/salt/scripts/loadbalancer_ip.sh:
  file.managed:
    - source: salt://loadbalancer/scripts/loadbalancer_ip.sh.j2
    - template: jinja
    - makedirs: True
    - user: root
    - group: root
    - mode: 700

create_loopback_service_unit:
  file.managed:
    - name: /etc/systemd/system/loopback.service
    - source: salt://loadbalancer/services/loopback.service.j2
    - template: jinja
    - makedirs: True
    - user: root
    - group: root
    - mode: 664
    - require:
      - file: /opt/salt/scripts/loadbalancer_ip.sh

start_and_enable_loopback_service:
  service.running:
    - name: loopback
    - enable: True
    - require:
      - file: /opt/salt/scripts/loadbalancer_ip.sh
      - file: /etc/systemd/system/loopback.service

{%- elif osMajorRelease == 9 %}

{%- set ip_list = salt['pillar.get']('freeipa:loadBalancer:ips', []) -%}
{%- set lo_nm_config = '/etc/NetworkManager/system-connections/lo.nmconnection' %}

manage_loopback_by_network_manager:
  cmd.run:
    - name: nmcli con add connection.id lo connection.type loopback connection.interface-name lo connection.autoconnect yes
    - failhard: True
    - unless: test -f {{ lo_nm_config }}

{% for ip in ip_list %}
add_{{ ip | replace(".", "_") }}_to_lo:
  cmd.run:
    - name: nmcli connection modify lo +ipv4.addresses {{ ip }}/32
    - unless: grep -q {{ ip }} {{ lo_nm_config }}
    - require:
      - cmd: manage_loopback_by_network_manager
{% endfor %}

refresh_lo:
  cmd.run:
    - name: nmcli con up lo
    - failhard: True
    - onchanges:
{%- for ip in ip_list %}
      - cmd: add_{{ ip | replace(".", "_") }}_to_lo
{%- endfor %}
{%- endif %}
{%- endif %}