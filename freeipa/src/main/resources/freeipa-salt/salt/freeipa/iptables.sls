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
ensure_filter_table:
  nftables.table_present:
    - name: filter
    - family: inet

ensure_input_chain:
  nftables.chain_present:
    - name: INPUT
    - table: filter
    - family: inet
    - table_type: filter
    - hook: input
    - priority: 0
    - require:
      - nftables: ensure_filter_table

accept_tcp_8443_on_lo:
  nftables.append:
    - position: 1
    - table: filter
    - chain: INPUT
    - family: inet
    - iifname: "lo"
    - proto: tcp
    - dport: 8443
    - jump: accept
    - save: True
    - unless: nft list chain inet filter INPUT | grep -q "tcp dport 8443 accept"
    - require:
      - nftables: ensure_input_chain

accept_tcp_8080_on_lo:
  nftables.append:
    - position: 2
    - table: filter
    - chain: INPUT
    - family: inet
    - iifname: "lo"
    - proto: tcp
    - dport: 8080
    - jump: accept
    - save: True
    - unless: nft list chain inet filter INPUT | grep -q "tcp 8080 8443 accept"
    - require:
      - nftables: ensure_input_chain

drop_tcp_8443_nftables:
  nftables.append:
    - position: 3
    - table: filter
    - chain: INPUT
    - family: inet
    - proto: tcp
    - dport: 8443
    - connstate: new  # Changed from ctstate to connstate for broader compatibility
    - jump: drop
    - save: True
    - unless: nft list chain inet filter INPUT | grep -q "tcp dport 8443 drop"
    - require:
      - nftables: ensure_input_chain
      - nftables: accept_tcp_8443_on_lo

drop_tcp_8080_nftables:
  nftables.append:
    - position: 4
    - table: filter
    - chain: INPUT
    - family: inet
    - proto: tcp
    - dport: 8080
    - connstate: new
    - jump: drop
    - save: True
    - unless: nft list chain inet filter INPUT | grep -q "tcp dport 8080 drop"
    - require:
      - nftables: ensure_input_chain
      - nftables: accept_tcp_8080_on_lo
{%- endif %}
{%- endif %}
