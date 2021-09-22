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
    - unless:
      - rpm -q ipa-client openldap openldap-clients

/opt/salt/scripts/join_ipa.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://sssd/template/join_ipa.j2
    - template: jinja

join_ipa:
  cmd.run:
{% if metadata.platform != 'YARN' %}
    - name: /opt/salt/scripts/join_ipa.sh && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/ipa-join-executed
{% else %}
    - name: runuser -l root -c 'export PW="{{salt['pillar.get']('sssd-ipa:password')}}" && /opt/salt/scripts/join_ipa.sh && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/ipa-join-executed'
{% endif %}
    - unless: test -f /var/log/ipa-join-executed && test -f /var/log/dnsrecord-add-executed
    - runas: root
    - failhard: True
    - require:
        - file: /opt/salt/scripts/join_ipa.sh
{% if metadata.platform != 'YARN' %}
    - env:
        - PW: "{{salt['pillar.get']('sssd-ipa:password')}}"
{% endif %}

{%- if salt['pillar.get']('freeipa:host', None) != None %}
{%- set freeipa_fqdn = salt['pillar.get']('freeipa:host') %}
update_default_server:
  file.replace:
    - name: /etc/ipa/default.conf
    - pattern: "server =.*"
    - repl: "server = {{ freeipa_fqdn }}"
    - require:
        - cmd: join_ipa

update_default_host:
  file.replace:
    - name: /etc/ipa/default.conf
    - pattern: "host =.*"
    - repl: "host = {{ freeipa_fqdn }}"
    - require:
        - cmd: join_ipa

update_default_xmlrpc_uri:
  file.replace:
    - name: /etc/ipa/default.conf
    - pattern: "xmlrpc_uri =.*"
    - repl: "xmlrpc_uri = https://{{ freeipa_fqdn }}/ipa/xml"
    - require:
        - cmd: join_ipa

{%- endif %}

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

/opt/salt/scripts/add_dns_record.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://sssd/template/add_dns_record.j2
    - template: jinja

add_dns_record:
  cmd.run:
    - name: /opt/salt/scripts/add_dns_record.sh 2>&1 | tee -a /var/log/dnsrecord-add.log && test ${PIPESTATUS[0]} -eq 0 && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/dnsrecord-add-executed
    - runas: root
    - failhard: True
    - unless: test -f /var/log/dnsrecord-add-executed
    - require:
        - file: /opt/salt/scripts/add_dns_record.sh
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
