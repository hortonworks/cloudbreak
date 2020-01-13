{%- from 'sssd/settings.sls' import ipa with context %}

ipa_packages_install:
  pkg.installed:
    - refresh: False
    - failhard: True
    - pkgs:
        - ipa-client
        - openldap
        - openldap-clients

join_ipa:
  cmd.run:
{% if not salt['file.directory_exists']('/yarn-private') %}
    - name: |
        ipa-client-install --realm={{salt['pillar.get']('sssd-ipa:realm')}} \
          --domain={{salt['pillar.get']('sssd-ipa:domain')}} --mkhomedir --principal={{salt['pillar.get']('sssd-ipa:principal')}} \
          {%- if "ID_BROKER_CLOUD_IDENTITY_ROLE" in grains.get('roles', []) %}
          --no-sshd --no-ssh \
          {%- endif %}
          --password $PW --unattended --force-join --ssh-trust-dns --no-ntp
{% else %}
    - name: |
        runuser -l root -c 'ipa-client-install --server={{salt['pillar.get']('sssd-ipa:server')}} --realm={{salt['pillar.get']('sssd-ipa:realm')}} \
          --domain={{salt['pillar.get']('sssd-ipa:domain')}} --mkhomedir --principal={{salt['pillar.get']('sssd-ipa:principal')}} \
          {%- if "ID_BROKER_CLOUD_IDENTITY_ROLE" in grains.get('roles', []) %}
          --no-sshd --no-ssh \
          {%- endif %}
          --password "{{salt['pillar.get']('sssd-ipa:password')}}" --unattended --force-join --ssh-trust-dns --no-ntp --unattended'
{% endif %}
    - unless: ipa env
    - runas: root
    - failhard: True
    - env:
        - PW: "{{salt['pillar.get']('sssd-ipa:password')}}"

{% if salt['file.directory_exists']('/yarn-private') %}
dns_remove_script:
  file.managed:
    - source: salt://sssd/ycloud/dns_cleanup.sh
    - name: /opt/salt/scripts/dns_cleanup.sh
    - user: root
    - group: root
    - mode: 755
    - template: jinja

removing_dns_entries:
  cmd.run:
    - name: runuser -l root -c '/opt/salt/scripts/dns_cleanup.sh 2>&1 | tee -a /var/log/dns_cleanup.log'
    - unless: test -f /var/log/dns_cleanup.log
{% endif %}

add_dns_record:
  cmd.run:
    - name: echo $PW | kinit {{salt['pillar.get']('sssd-ipa:principal')}} && ipa dnsrecord-add {{salt['pillar.get']('sssd-ipa:domain')}}. $(hostname) --a-rec=$(hostname -i) --a-create-reverse
    - unless: echo $PW | kinit {{salt['pillar.get']('sssd-ipa:principal')}} && ipa dnsrecord-find {{salt['pillar.get']('sssd-ipa:domain')}} --a-rec=$(hostname -i)
    - env:
        - PW: "{{salt['pillar.get']('sssd-ipa:password')}}"

{% if not salt['file.directory_exists']('/yarn-private') %}

restart_sssd_if_reconfigured:
  service.running:
    - enable: True
    - name: sssd
    - watch:
      - file: /etc/sssd/sssd.conf

{% endif %}

/etc/sssd/sssd.conf:
  file.managed:
    - source: salt://sssd/template/sssd-ipa.j2
    - template: jinja
    - mode: 700
    - user: root
    - group: root

{% if salt['file.directory_exists']('/yarn-private') %}

backup_systemctl:
  file.copy:
    - name: /usr/bin/systemctl.bak
    - source: /usr/bin/systemctl.orig

replace_systemctl:
  file.managed:
    - source: salt://sssd/ycloud/systemctl
    - name: /usr/bin/systemctl.orig
    - user: root
    - group: root
    - mode: 755

force_restart_ssd:
  cmd.run:
    - name: runuser -l root -c 'systemctl restart sssd'
    - runas: root
    - require:
        - file: replace_systemctl

restore_systemctl:
  file.copy:
    - name: /usr/bin/systemctl.orig
    - source: /usr/bin/systemctl.bak
    - user: root
    - group: root
    - mode: 755
    - force: True
{% endif %}

include:
    - sssd.ssh
