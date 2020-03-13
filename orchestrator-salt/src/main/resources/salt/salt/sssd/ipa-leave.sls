{%- from 'sssd/settings.sls' import ipa with context %}
{%- from 'metadata/settings.sls' import metadata with context %}

{% if metadata.platform == 'YARN' and not metadata.cluster_in_childenvironment %}
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
        - password: "{{salt['pillar.get']('sssd-ipa:password')}}"
    - onlyif: ls /etc/cloudera-scm-server/cmf.keytab
    - require:
      - file: create_remove_cm_sa_script

{%- endif %}
{% endif %}

leave-ipa:
  cmd.run:
{% if metadata.platform != 'YARN' %}
    - name: ipa host-del {{ salt['grains.get']('fqdn') }} --updatedns && ipa-client-install --uninstall -U
{% else %}
    - name: runuser -l root -c 'ipa host-del {{ salt['grains.get']('fqdn') }} --updatedns && ipa-client-install --uninstall -U'
{% endif %}
    - onlyif: ipa env
