{%- from 'sssd/settings.sls' import ipa with context %}
{%- from 'metadata/settings.sls' import metadata with context %}

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
{% if metadata.platform != 'YARN' %}
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
          --password "{{salt['pillar.get']('sssd-ipa:password')}}" --unattended --force-join --ssh-trust-dns --no-ntp'
{% endif %}
    - unless: echo $PW | kinit {{salt['pillar.get']('sssd-ipa:principal')}} && ipa env
    - runas: root
    - failhard: True
    - env:
        - PW: "{{salt['pillar.get']('sssd-ipa:password')}}"

{% if metadata.platform == 'YARN' and not metadata.cluster_in_childenvironment %}
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

{% if metadata.platform != 'YARN' %}

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

{% if metadata.platform == 'YARN' %}

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

dns_resolution_fix:
  file.append:
    - name: /etc/resolv.conf
    - text: "domain {{ salt['pillar.get']('hosts')[metadata.server_address]['domain'] }}"
{% endif %}

include:
    - sssd.ssh

{% if metadata.platform == 'YARN' and not metadata.cluster_in_childenvironment %}
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
    - name: newgrp root && sh /opt/salt/scripts/generate_cm_keytab.sh 2>&1 | tee -a /var/log/generate_cm_keytab.log && exit ${PIPESTATUS[0]}
    - runas: root
    - env:
        - password: "{{salt['pillar.get']('sssd-ipa:password')}}"
    - unless: ls /etc/cloudera-scm-server/cmf.keytab
    - require:
      - file: create_cm_keytab_generation_script

{%- endif %}
{% endif %}
