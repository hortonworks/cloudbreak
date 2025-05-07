{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}
{%- from 'gateway/settings.sls' import gateway with context %}
{%- from 'postgresql/settings.sls' import postgresql with context %}

{% set cloudera_manager_database_connection_url = 'jdbc:' ~ cloudera_manager.cloudera_manager_database.subprotocol ~ '://' ~ cloudera_manager.cloudera_manager_database.host ~ '/' ~ cloudera_manager.cloudera_manager_database.databaseName ~ '?sslmode=' ~ postgresql.ssl_verification_mode ~ '&sslrootcert=' ~ postgresql.root_certs_file %}

init-cloudera-manager-db:
  cmd.run:
{%- if postgresql.ssl_enabled == True and postgresql.ssl_for_cm_db_natively_supported == True %}
    - name: /opt/cloudera/cm/schema/scm_prepare_database.sh -s -j "{{ cloudera_manager_database_connection_url }}" -h {{ cloudera_manager.cloudera_manager_database.host }} {{ cloudera_manager.cloudera_manager_database.subprotocol }} {{ cloudera_manager.cloudera_manager_database.databaseName }} $user $pass && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/init-cloudera-manager-db-executed
{%- else %}
    - name: /opt/cloudera/cm/schema/scm_prepare_database.sh -h {{ cloudera_manager.cloudera_manager_database.host }} {{ cloudera_manager.cloudera_manager_database.subprotocol }} {{ cloudera_manager.cloudera_manager_database.databaseName }} $user $pass && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/init-cloudera-manager-db-executed
{%- endif %}
    - unless: test -f /var/log/init-cloudera-manager-db-executed
    - env:
        - user: {{ cloudera_manager.cloudera_manager_database.connectionUserName }}
        - pass: {{ cloudera_manager.cloudera_manager_database.connectionPassword }}
{%- if postgresql.ssl_enabled == True and postgresql.ssl_for_cm_db_natively_supported == True %}
    - require:
        - file: {{ postgresql.root_certs_file }}
{%- endif %}

# Configure JDBC URL for CM after DB init if client side DB server certificate validation is enabled for the cluster and the CM version is too old to support this natively
{% if postgresql.ssl_enabled == True and postgresql.ssl_for_cm_db_natively_supported == False %}
replace-db-connection-url:
  file.replace:
    - name: /etc/cloudera-scm-server/db.properties
    - pattern: "(#UPDATED BY CDP CP:\n)?com.cloudera.cmf.orm.hibernate.connection.url=.*"
    - repl: "#UPDATED BY CDP CP:\ncom.cloudera.cmf.orm.hibernate.connection.url={{ cloudera_manager_database_connection_url }}"
    - append_if_not_found: True
    - require:
        - cmd: init-cloudera-manager-db
        - file: {{ postgresql.root_certs_file }}
{% endif %}

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

{% if salt['pillar.get']('cloudera-manager:mgmt_service_directories') is defined and salt['pillar.get']('cloudera-manager:mgmt_service_directories')|length > 0 %}
{%- for dir in salt['pillar.get']('cloudera-manager:mgmt_service_directories') %}
ensure_owners_{{ dir }}:
  cmd.run:
    - name: >
        if test -d {{ dir }}; then
          chown -hfR cloudera-scm:cloudera-scm {{ dir }}
        fi

{%- endfor %}
{% endif %}

start_server:
  service.running:
    - enable: True
    - name: cloudera-scm-server
{% if "ipa_member" in grains.get('roles', []) %}
    - require:
        - pkg: ipa_packages_install
{% endif %}