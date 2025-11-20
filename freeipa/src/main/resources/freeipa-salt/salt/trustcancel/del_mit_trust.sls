remove_mit_krb5_conf:
  file.absent:
    - name: /etc/krb5.conf.d/mit_trust.conf

create_del_mit_principals_script:
  file.managed:
    - name: /opt/salt/scripts/del_mit_principals.sh
    - source: salt://trustcancel/scripts/del_mit_principals.j2
    - template: jinja
    - makedirs: True
    - user: root
    - group: root
    - mode: 755

del_mit_principals:
  cmd.run:
    - name: sh /opt/salt/scripts/del_mit_principals.sh 2>&1 | tee -a /var/log/del_mit_principals.log && exit ${PIPESTATUS[0]}
    - env:
        - FPW: "{{salt['pillar.get']('freeipa:password')}}"
    - require:
      - file: create_del_mit_principals_script
    - failhard: True