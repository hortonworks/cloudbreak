{%- set ad_domain = salt['pillar.get']('freeipa:trust_setup:ad_domain', '') -%}
{%- set ad_ip = salt['pillar.get']('freeipa:trust_setup:ad_ip', '') -%}
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
    - name: /opt/salt/scripts/ad_dns_validation.sh {{ ad_domain }} {{ ad_ip }} {{ ad_ip }}
    - failhard: True
    - require:
      - file: /opt/salt/scripts/ad_dns_validation.sh
