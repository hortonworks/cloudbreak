{%- from 'sssd/settings.sls' import ipa with context %}

packages_install:
  pkg.installed:
    - refresh: False
    - pkgs:
        - ipa-client
        - openldap
        - openldap-clients

join_ipa:
  cmd.run:
    - name: |
        ipa-client-install --server={{salt['pillar.get']('sssd-ipa:server')}} --realm={{salt['pillar.get']('sssd-ipa:realm')}} \
          --domain={{salt['pillar.get']('sssd-ipa:domain')}} --mkhomedir --principal={{salt['pillar.get']('sssd-ipa:principal')}} \
          --password $PW --unattended --force-join --ssh-trust-dns --force-ntpd
    - unless: ipa env
    - env:
        - PW: {{salt['pillar.get']('sssd-ipa:password')}}

add_dns_record:
  cmd.run:
    - name: echo $PW | kinit {{salt['pillar.get']('sssd-ipa:principal')}} && ipa dnsrecord-add {{salt['pillar.get']('sssd-ipa:domain')}}. $(hostname) --a-rec=$(hostname -i) --a-create-reverse
    - unless: echo $PW | kinit {{salt['pillar.get']('sssd-ipa:principal')}} && ipa dnsrecord-find {{salt['pillar.get']('sssd-ipa:domain')}} --a-rec=$(hostname -i)
    - env:
        - PW: {{salt['pillar.get']('sssd-ipa:password')}}

restart-sssd-if-reconfigured:
  service.running:
    - enable: True
    - name: sssd
    - watch:
      - file: /etc/sssd/sssd.conf

/etc/sssd/sssd.conf:
  file.managed:
    - source: salt://sssd/template/sssd-ipa.j2
    - template: jinja
    - mode: 700

include:
    - sssd.ssh

{%- if "manager_server" in grains.get('roles', []) %}

create_cm_keytab_generation_script:
  file.managed:
    - name: /opt/salt/scripts/generate_cm_keytab.sh
    - source: salt://sssd/template/generate_cm_keytab.j2
    - makedirs: True
    - template: jinja
    - context:
        ipa: {{ ipa }}
    - mode: 755

generate_cm_freeipa_keytab:
  cmd.run:
    - name: sh /opt/salt/scripts/generate_cm_keytab.sh 2>&1 | tee -a /var/log/generate_cm_keytab.log && exit ${PIPESTATUS[0]}
    - env:
        - password: {{salt['pillar.get']('sssd-ipa:password')}}
    - unless: ls /etc/cloudera-scm-server/cmf.keytab
    - require:
      - file: create_cm_keytab_generation_script

{%- endif %}