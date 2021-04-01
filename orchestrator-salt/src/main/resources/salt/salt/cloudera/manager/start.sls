{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}
{%- from 'gateway/settings.sls' import gateway with context %}
{%- from 'postgresql/settings.sls' import postgresql with context %}

init-cloudera-manager-db:
  cmd.run:
    - name: /opt/cloudera/cm/schema/scm_prepare_database.sh -h {{ cloudera_manager.cloudera_manager_database.host }} {{ cloudera_manager.cloudera_manager_database.subprotocol }} {{ cloudera_manager.cloudera_manager_database.databaseName }} $user $pass && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/init-cloudera-manager-db-executed
    - unless: test -f /var/log/init-cloudera-manager-db-executed
    - env:
        - user: {{ cloudera_manager.cloudera_manager_database.connectionUserName }}
        - pass: {{ cloudera_manager.cloudera_manager_database.connectionPassword }}

#Configure JDBC URL for CM if client side certification validation is enabled for the Datalake cluster
{% if postgresql.ssl_enabled == True and salt['pillar.get']('telemetry:clusterType') == "datalake" %}
replace-db-connection-url:
  file.replace:
    - name: /etc/cloudera-scm-server/db.properties
    - pattern: "(#UPDATED BY CDP CP:\n)?com.cloudera.cmf.orm.hibernate.connection.url=.*"
    - repl: "#UPDATED BY CDP CP:\ncom.cloudera.cmf.orm.hibernate.connection.url=jdbc:{{ cloudera_manager.cloudera_manager_database.subprotocol }}://{{ cloudera_manager.cloudera_manager_database.host }}/{{ cloudera_manager.cloudera_manager_database.databaseName }}?sslmode=verify-full&sslrootcert={{ postgresql.root_certs_file }}"
    - append_if_not_found: True
    - require:
        - cmd: init-cloudera-manager-db
{% endif %}

# We need to restart the CM if the frontend URL has been changed (e.g. switching to PEM based DNS name)

{% if 'cm_primary' in grains.get('roles', []) %}
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
{% endif %}

{% if salt['pillar.get']('cloudera-manager:mgmt_service_directories') is defined and salt['pillar.get']('cloudera-manager:mgmt_service_directories')|length > 0 %}
{%- for dir in salt['pillar.get']('cloudera-manager:mgmt_service_directories') %}
ensure_owners_{{ dir }}:
  file.directory:
    - name: {{ dir }}
    - user: cloudera-scm
    - group: cloudera-scm
    - recurse:
      - user
      - group
    - onlyif: test -d {{ dir }}

{%- endfor %}
{% endif %}

{% if 'cm_primary' in grains.get('roles', []) %}
start_server:
  service.running:
    - enable: True
    - name: cloudera-scm-server
{% if "ipa_member" in grains.get('roles', []) %}
    - require:
        - pkg: ipa_packages_install
{% endif %}
{% endif %}