{%- from 'kerberos/settings.sls' import kerberos with context %}

haveged:
  pkg.installed: []
  service.running:
    - enable: True

install_kerberos:
  pkg.installed:
    - pkgs:
      - krb5-server
      - krb5-libs
      - krb5-workstation

{% if kerberos.url is none or kerberos.url == '' %}

{% if salt['cmd.retcode']('test -f /var/krb5-conf-initialized') == 1 %}
/etc/krb5.conf:
  file.managed:
    - source: salt://kerberos/config/krb5.conf
    - template: jinja
{% endif %}

create_db:
  cmd.run:
    - name: /usr/sbin/kdb5_util -P {{ kerberos.master_key }} -r {{ kerberos.realm }} create -s
    - unless: ls -la /var/kerberos/krb5kdc/principal
    - watch:
      - pkg: install_kerberos

create_admin_user:
  cmd.run:
    - name: 'kadmin.local -q "addprinc -pw {{ kerberos.password }} {{ kerberos.user }}/admin"'
    - shell: /bin/bash
    - unless: kadmin.local -q "list_principals *" | grep "*/admin@{{ kerberos.realm }} *"

create_cluster_user:
  cmd.run:
    - name: 'kadmin.local -q "addprinc -pw {{ kerberos.clusterPassword }} {{ kerberos.clusterUser }}"'
    - shell: /bin/bash
    - unless: kadmin.local -q "list_principals *" | grep "^{{ kerberos.clusterUser }}@{{ kerberos.clusterPassword }} *"

/var/kerberos/krb5kdc/kadm5.acl:
  cmd.run:
    - name: 'echo "*/admin@{{ kerberos.realm }} *" >> /var/kerberos/krb5kdc/kadm5.acl'
    - shell: /bin/bash
    - unless:  cat /var/kerberos/krb5kdc/kadm5.acl | grep "*/admin@{{ kerberos.realm }} *"

start_kadmin:
  service.running:
    - enable: True
    - name: kadmin
    - watch:
        - pkg: install_kerberos

start_kdc:
  service.running:
    - enable: True
    - name: krb5kdc
    - watch:
      - pkg: install_kerberos

{% else %}

{% if salt['cmd.retcode']('test -f /var/krb5-conf-initialized') == 1 %}
/etc/krb5.conf:
  file.managed:
    - source: salt://kerberos/config/krb5.conf-existing
    - template: jinja
{% endif %}

{% endif %}

create_krb5_conf_initialized:
  cmd.run:
    - name: touch /var/krb5-conf-initialized
    - shell: /bin/bash
    - unless: test -f /var/krb5-conf-initialized