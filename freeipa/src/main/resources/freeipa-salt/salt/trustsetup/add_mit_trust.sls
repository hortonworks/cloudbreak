/etc/krb5.conf.d/mit_trust.conf:
  file.managed:
    - source: salt://trustsetup/config/mit_trust.conf.j2
    - template: jinja

create_add_mit_principals_script:
  file.managed:
    - name: /opt/salt/scripts/add_mit_principals.sh
    - source: salt://trustsetup/scripts/add_mit_principals.j2
    - template: jinja
    - makedirs: True
    - user: root
    - group: root
    - mode: 755

add_mit_principals:
  cmd.run:
    - name: sh /opt/salt/scripts/add_mit_principals.sh 2>&1 | tee -a /var/log/add_mit_principals.log && exit ${PIPESTATUS[0]}
    - env:
      - FPW: "{{salt['pillar.get']('freeipa:password')}}"
    - require:
      - file: create_add_mit_principals_script
    - failhard: True