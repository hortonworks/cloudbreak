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