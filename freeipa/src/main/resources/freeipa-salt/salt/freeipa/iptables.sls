{%- set os = salt['grains.get']('os') %}
{%- set osMajorRelease = salt['grains.get']('osmajorrelease') | int %}

{%- if os == 'RedHat' %}
{%- if osMajorRelease == 8 %}
accept_tcp_8443_on_lo:
  iptables.insert:
    - position: 1
    - table: filter
    - chain: INPUT
    - jump: ACCEPT
    - protocol: tcp
    - dport: 8443
    - i: lo
    - save: True
    - unless: iptables -L INPUT -n | grep -q "8443 -j ACCEPT"

accept_tcp_8080_on_lo:
  iptables.insert:
    - position: 2
    - table: filter
    - chain: INPUT
    - jump: ACCEPT
    - protocol: tcp
    - dport: 8080
    - i: lo
    - save: True
    - unless: iptables -L INPUT -n | grep -q "8080 -j ACCEPT"

reject_tcp_8443_iptables:
  iptables.insert:
    - position: 3
    - table: filter
    - chain: INPUT
    - jump: DROP
    - protocol: tcp
    - dport: 8443
    - match: state
    - connstate: NEW
    - save: True
    - unless: iptables -L INPUT -n | grep -q "8443 -j DROP"

reject_tcp_8080_iptables:
  iptables.insert:
    - position: 4
    - table: filter
    - chain: INPUT
    - jump: DROP
    - dport: 8080
    - protocol: tcp
    - match: state
    - connstate: NEW
    - save: True
    - unless: iptables -L INPUT -n | grep -q "8080 -j DROP"

save_iptables_rules:
  module.run:
    - name: iptables.save
    - filename: /etc/sysconfig/iptables
{%- elif osMajorRelease >= 9 %}
deploy_freeipa_nftables_conf:
  file.managed:
    - name: /etc/nftables.d/freeipa.conf
    - source: salt://freeipa/templates/freeipa-nftables.conf
    - mode: 600
    - user: root
    - group: root
    - makedirs: True

include_freeipa_nftables_conf:
  file.append:
    - name: /etc/sysconfig/nftables.conf
    - text: 'include "/etc/nftables.d/freeipa.conf"'
    - require:
      - file: deploy_freeipa_nftables_conf

nftables:
  service.running:
    - enable: True
    - require:
      - file: include_freeipa_nftables_conf
    - watch:
      - file: deploy_freeipa_nftables_conf
      - file: include_freeipa_nftables_conf
{%- endif %}
{%- endif %}
