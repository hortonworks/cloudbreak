{%- set kdc_fqdn = salt['pillar.get']('freeipa:trust_setup:kdc_fqdn', '') -%}
{%- set kdc_ip = salt['pillar.get']('freeipa:trust_setup:kdc_ip', '') -%}
{%- set dns_ip = salt['pillar.get']('freeipa:trust_setup:dns_ip', '') -%}
/opt/salt/scripts/ad_dns_validation.sh:
  file.managed:
    - source: salt://trustsetup/validation/scripts/ad_dns_validation.sh.j2
    - template: jinja
    - makedirs: True
    - user: root
    - group: root
    - mode: 700

run-ad-dns-validation:
  cmd.run:
    - name: /opt/salt/scripts/ad_dns_validation.sh {{ kdc_fqdn }} {{ dns_ip }} {{ kdc_ip }}
    - failhard: True
    - require:
      - file: /opt/salt/scripts/ad_dns_validation.sh
