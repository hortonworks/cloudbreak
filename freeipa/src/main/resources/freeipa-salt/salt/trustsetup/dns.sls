{%- set kdc_fqdn = salt['pillar.get']('freeipa:trust_setup:kdc_fqdn') %}
{%- set kdc_realm = salt['pillar.get']('freeipa:trust_setup:kdc_realm') %}

validate_srv_records:
  cmd.run:
    - name: |
        dig +short -t SRV _ldap._tcp.{{ kdc_realm }}
        dig +short -t SRV _ldap._udp.{{ kdc_realm }}
        dig +short -t SRV _kerberos._tcp.{{ kdc_realm }}
        dig +short -t SRV _kerberos._udp.{{ kdc_realm }}
        dig +short -t SRV _kerberos._tcp.{{ kdc_fqdn | upper }}.
        dig +short -t SRV _kerberos._udp.{{ kdc_fqdn | upper }}.
        dig +short -t SRV _ldap._tcp.{{ kdc_fqdn | upper }}.
        dig +short -t SRV _ldap._udp.{{ kdc_fqdn | upper }}.
        dig +short -t SRV _ldap._tcp.dc._msdcs.{{ kdc_fqdn }}.
        dig +short -t SRV _kerberos._tcp.dc._msdcs.{{ kdc_fqdn }}.
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
