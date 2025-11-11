/opt/salt/scripts/trust_status_validation.sh:
  file.managed:
    - source: salt://trustsetup/validation/scripts/trust_status_validation.sh.j2
    - template: jinja
    - makedirs: True
    - user: root
    - group: root
    - mode: 700

run-trust-status-validation:
  cmd.run:
    - name: /opt/salt/scripts/trust_status_validation.sh
    - env:
      - ADMIN_PASSWORD: {{ salt['pillar.get']('freeipa:password') }}
      - KDC_FQDN: {{ salt['pillar.get']('freeipa:trust_setup:kdc_fqdn', '') }}
      - KDC_REALM: {{ salt['pillar.get']('freeipa:trust_setup:kdc_realm', '') }}
    - failhard: True
    - require:
      - file: /opt/salt/scripts/trust_status_validation.sh
