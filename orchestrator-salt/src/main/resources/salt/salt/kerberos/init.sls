{%- from 'kerberos/settings.sls' import kerberos with context %}

haveged:
  pkg.installed: []
  service.running:
    - enable: True

/etc/krb5.conf:
  file.managed:
    - source: salt://kerberos/config/krb5.conf
    - template: jinja

install_kerberos:
  pkg.installed:
    - pkgs:
      - krb5-server
      - krb5-libs
      - krb5-workstation

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