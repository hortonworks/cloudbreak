{%- from 'kerberos/settings.sls' import kerberos with context %}

include:
  - {{ slspath }}.common

{% if kerberos.url is none or kerberos.url == '' %}

add_principals_sh_script:
  file.managed:
    - name: /tmp/principals.sh
    - source: salt://kerberos/scripts/principals.sh
    - template: jinja
    - skip_verify: True
    - makedirs: True
    - mode: 755
    - context:
      realm: {{ kerberos.realm }}
      kdcs: {{ kerberos.kdcs }}
      pw: {{ kerberos.password }}
      usr: {{ kerberos.user }}

run_principals_sh_script:
  cmd.run:
    - name: sh -x /tmp/principals.sh 2>&1 | tee -a /var/log/principals_sh.log && exit ${PIPESTATUS[0]}
    - require:
      - file: add_principals_sh_script
    - output_loglevel: quiet

remove_kprop_acl:
  file.absent:
    - name: /var/kerberos/krb5kdc/kpropd.acl

start_kadmin:
  service.running:
    - enable: True
    - name: kadmin
    - watch:
        - pkg: install_kerberos

start_master_kdc:
  service.running:
    - enable: True
    - name: krb5kdc
    - watch:
      - pkg: install_kerberos

stop_kpropd:
  service.dead:
    - enable: False
    - name: kpropd

{% endif %}

create_krb5_conf_initialized:
  cmd.run:
    - name: touch /var/krb5-conf-initialized
    - shell: /bin/bash
    - unless: test -f /var/krb5-conf-initialized