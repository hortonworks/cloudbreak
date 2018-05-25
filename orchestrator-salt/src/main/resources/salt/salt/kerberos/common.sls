{%- from 'kerberos/settings.sls' import kerberos with context %}

haveged:
  pkg.installed: []
  service.running:
    - enable: True

{% if grains['os_family'] == 'Suse' %}
install_kerberos:
  pkg.installed:
    - pkgs:
      - krb5-server
{% elif grains['os_family'] == 'Debian' %}
install_kerberos:
  pkg.installed:
    - pkgs:
      - krb5-kdc
      - krb5-admin-server
{% else %}
install_kerberos:
  pkg.installed:
    - pkgs:
      - krb5-server
      - krb5-libs
      - krb5-workstation
{% endif %}

{% if grains['os_family'] == 'Suse' %}
/var/kerberos:
  file.symlink:
      - target: /var/lib/kerberos
      - force: True
{% endif %}

{% if grains['os_family'] == 'Debian' %}
/var/kerberos:
  file.symlink:
      - target: /etc
      - force: True
{% endif %}

{% if kerberos.url is none or kerberos.url == '' %}

{% if salt['cmd.retcode']('test -f /var/krb5-conf-initialized') == 1 %}
/etc/krb5.conf:
  file.managed:
    - source: salt://kerberos/config/krb5.conf
    - template: jinja
    - context:
      enable_iprop: {{ kerberos.enable_iprop }}
{% endif %}

create_db:
  cmd.run:
{% if grains['os_family'] == 'Suse' %}
    - name: /usr/lib/mit/sbin/kdb5_util -P {{ kerberos.master_key }} -r {{ kerberos.realm }} create -s
{% else %}
    - name: /usr/sbin/kdb5_util -P {{ kerberos.master_key }} -r {{ kerberos.realm }} create -s
{% endif %}
    - unless: ls -la /var/kerberos/krb5kdc/principal
    - watch:
      - pkg: install_kerberos
    - output_loglevel: quiet

add_kadm5_sh_script:
  file.managed:
    - name: /opt/salt/kadm5.sh
    - source: salt://kerberos/scripts/kadm5.sh
    - template: jinja
    - skip_verify: True
    - makedirs: True
    - mode: 755
    - context:
      realm: {{ kerberos.realm }}
      kdcs: {{ kerberos.kdcs }}

run_kadm5_sh_script:
  cmd.run:
    - name: sh -x /opt/salt/kadm5.sh 2>&1 | tee -a /var/log/kadm5_sh.log && exit ${PIPESTATUS[0]}
    - require:
      - file: add_kadm5_sh_script

/etc/init.d/kpropd:
  file.managed:
    - makedirs: True
    - source: salt://kerberos/init.d/kpropd
    - mode: 755

{% if grains['os_family'] == 'Suse' %}
create_cluster_user:
  cmd.run:
    - name: '/usr/lib/mit/sbin/kadmin.local -q "addprinc -pw {{ kerberos.clusterPassword }} {{ kerberos.clusterUser }}"'
    - shell: /bin/bash
    - unless: /usr/lib/mit/sbin/kadmin.local -q "list_principals *" | grep "^{{ kerberos.clusterUser }}@{{ kerberos.clusterPassword }} *"
    - output_loglevel: quiet
{% else %}
create_cluster_user:
  cmd.run:
    - name: 'kadmin.local -q "addprinc -pw {{ kerberos.clusterPassword }} {{ kerberos.clusterUser }}"'
    - shell: /bin/bash
    - unless: kadmin.local -q "list_principals *" | grep "^{{ kerberos.clusterUser }}@{{ kerberos.clusterPassword }} *"
    - output_loglevel: quiet
{% endif %}

{% if grains['init'] == 'systemd' %}

kpropd_service:
  file.managed:
    - name: /etc/systemd/system/kpropd.service
    - source: salt://kerberos/systemd/kpropd.service

{% endif %}

{% else %}

{% if salt['cmd.retcode']('test -f /var/krb5-conf-initialized') == 1 %}
/etc/krb5.conf:
  file.managed:
    - source: salt://kerberos/config/krb5.conf-existing
    - template: jinja
{% endif %}

{% endif %}