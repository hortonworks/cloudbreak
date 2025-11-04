{%- set kdc_domain = salt['pillar.get']('freeipa:trust_setup:kdc_domain') %}
{%- set kdc_realm = salt['pillar.get']('freeipa:trust_setup:kdc_realm') %}

validate_srv_records:
  cmd.run:
    - name: |
        dig +short -t SRV _ldap._tcp.{{ kdc_realm }}
        dig +short -t SRV _ldap._udp.{{ kdc_realm }}
        dig +short -t SRV _kerberos._tcp.{{ kdc_realm }}
        dig +short -t SRV _kerberos._udp.{{ kdc_realm }}
        dig +short -t SRV _kerberos._tcp.{{ kdc_domain | upper }}.
        dig +short -t SRV _kerberos._udp.{{ kdc_domain | upper }}.
        dig +short -t SRV _ldap._tcp.{{ kdc_domain | upper }}.
        dig +short -t SRV _ldap._udp.{{ kdc_domain | upper }}.
        dig +short -t SRV _ldap._tcp.dc._msdcs.{{ kdc_domain }}.
        dig +short -t SRV _kerberos._tcp.dc._msdcs.{{ kdc_domain }}.
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
