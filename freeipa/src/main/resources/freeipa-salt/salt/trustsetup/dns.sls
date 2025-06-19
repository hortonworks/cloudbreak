{%- set ad_domain = salt['pillar.get']('freeipa:trust_setup:ad_domain') %}
{%- set realm = salt['pillar.get']('freeipa:trust_setup:realm') %}

validate_srv_records:
  cmd.run:
    - name: |
        dig +short -t SRV _ldap._tcp.{{ realm }}
        dig +short -t SRV _ldap._udp.{{ realm }}
        dig +short -t SRV _kerberos._tcp.{{ realm }}
        dig +short -t SRV _kerberos._udp.{{ realm }}
        dig +short -t SRV _kerberos._tcp.{{ ad_domain | upper }}.
        dig +short -t SRV _kerberos._udp.{{ ad_domain | upper }}.
        dig +short -t SRV _ldap._tcp.{{ ad_domain | upper }}.
        dig +short -t SRV _ldap._udp.{{ ad_domain | upper }}.
        dig +short -t SRV _ldap._tcp.dc._msdcs.{{ ad_domain }}.
        dig +short -t SRV _kerberos._tcp.dc._msdcs.{{ ad_domain }}.
    - failhard: True

reset_sssd_cache:
  service.dead:
    - name: sssd

clear_sssd_cache:
  file.directory:
    - name: /var/lib/sss/db
    - clean: true
    - require:
      - service: reset_sssd_cache

start_sssd:
  service.running:
    - name: sssd
    - require:
      - file: clear_sssd_cache
