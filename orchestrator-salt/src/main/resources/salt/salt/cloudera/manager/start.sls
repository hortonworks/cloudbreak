{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}
{%- from 'gateway/settings.sls' import gateway with context %}

init-cloudera-manager-db:
  cmd.run:
    - name: /opt/cloudera/cm/schema/scm_prepare_database.sh -h {{ cloudera_manager.cloudera_manager_database.host }} {{ cloudera_manager.cloudera_manager_database.subprotocol }} {{ cloudera_manager.cloudera_manager_database.databaseName }} $user $pass && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/init-cloudera-manager-db-executed
    - unless: test -f /var/log/init-cloudera-manager-db-executed
    - env:
        - user: {{ cloudera_manager.cloudera_manager_database.connectionUserName }}
        - pass: {{ cloudera_manager.cloudera_manager_database.connectionPassword }}

# We need to restart the CM if the frontend URL has been changed (e.g. switching to PEM based DNS name)

{% if salt['pillar.get']('gateway:userfacingfqdn') is defined and salt['pillar.get']('gateway:userfacingfqdn')|length > 1 %}
update_frontend_url_of_cm:
  file.replace:
      - name: /etc/cloudera-scm-server/cm.settings
      - pattern: 'setsettings FRONTEND_URL.*'
      - repl: 'setsettings FRONTEND_URL https://{{ salt['pillar.get']('gateway:userfacingfqdn') }}/'
      - unless: grep "^setsettings FRONTEND_URL https://{{ salt['pillar.get']('gateway:userfacingfqdn') }}/$" /etc/cloudera-scm-server/cm.settings

restart_cm_after_fronted_url_change:
  service.running:
    - name: cloudera-scm-server
{% if "ipa_member" in grains.get('roles', []) %}
    - require:
        - pkg: ipa_packages_install
{% endif %}
    - watch:
      - file: update_frontend_url_of_cm
{% endif %}

start_server:
  service.running:
    - enable: True
    - name: cloudera-scm-server
{% if "ipa_member" in grains.get('roles', []) %}
    - require:
        - pkg: ipa_packages_install
{% endif %}