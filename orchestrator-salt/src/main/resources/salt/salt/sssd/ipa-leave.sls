{%- from 'sssd/settings.sls' import ipa with context %}

{%- if "manager_server" in grains.get('roles', []) %}

create_remove_cm_sa_script:
  file.managed:
    - name: /opt/salt/scripts/remove_cm_sa.sh
    - source: salt://sssd/template/remove_cm_sa.j2
    - makedirs: True
    - template: jinja
    - context:
        ipa: {{ ipa }}
    - mode: 755

remove_cm_service_account:
  cmd.run:
    - name: sh /opt/salt/scripts/remove_cm_sa.sh 2>&1 | tee -a /var/log/remove_cm_sa.log && exit ${PIPESTATUS[0]}
    - env:
        - password: {{salt['pillar.get']('sssd-ipa:password')}}
    - onlyif: ls /etc/cloudera-scm-server/cmf.keytab
    - require:
      - file: create_remove_cm_sa_script

{%- endif %}

leave-ipa:
  cmd.run:
    - name: ipa host-del {{ salt['grains.get']('fqdn') }} --updatedns && ipa-client-install --uninstall -U
    - onlyif: ipa env
