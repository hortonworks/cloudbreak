{%- from 'metadata/settings.sls' import metadata with context %}
{%- from 'postgresql/settings.sls' import postgresql with context %}

{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}
{% set postgres_directory = salt['pillar.get']('postgres:postgres_directory') %}
{% set postgres_log_directory = salt['pillar.get']('postgres:postgres_log_directory') %}
{% set postgres_scripts_executed_directory = salt['pillar.get']('postgres:postgres_scripts_executed_directory') %}
{% set postgres_data_on_attached_disk = salt['pillar.get']('postgres:postgres_data_on_attached_disk', 'False') %}
{% set command = 'systemctl show -p FragmentPath postgresql' %}
{% set unitFile = salt['cmd.run'](command) | replace("FragmentPath=","") %}

{% if 'None' != configure_remote_db %}

include:
{%- if salt[ 'pillar.get' ]('postgres:postgres_version', '10') | int == 11 %}
{%- if not salt['file.file_exists']('/usr/pgsql-11/bin/psql') %}
  - postgresql.pg11-install
{%- endif %}
  - postgresql.pg11-alternatives
{%- endif %}
  - postgresql.disaster_recovery.recover

/opt/salt/scripts/init_db_remote.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: postgres
    - mode: 750
    - source: salt://postgresql/scripts/init_db_remote.sh
    - template: jinja

/opt/salt/scripts/init-services-db-remote-executed:
  file.rename:
    - makedirs: True
    - source: /var/log/init-services-db-remote-executed
    - unless: test -f opt/salt/scripts/init-services-db-remote-executed

init-services-db-remote:
  cmd.run:
    - name: runuser -l postgres -s /bin/bash -c '/opt/salt/scripts/init_db_remote.sh' && echo $(date +%Y-%m-%d:%H:%M:%S) >> /opt/salt/scripts/init-services-db-remote-executed
    - unless: test -f /opt/salt/scripts/init-services-db-remote-executed
    - require:
      - file: /opt/salt/scripts/init_db_remote.sh
{% if postgresql.ssl_enabled == True %}
      - file: {{ postgresql.root_certs_file }}
{%- endif %}

{%- else %}

{%- if postgres_data_on_attached_disk %}

{{ postgres_directory }}:
  file.directory:
    - user: postgres
    - group: postgres
    - mode: 700

{{ postgres_log_directory }}:
  file.directory:
    - user: root
    - group: root
    - mode: 755

{{ postgres_scripts_executed_directory }}:
  file.directory:
    - user: root
    - group: root
    - mode: 755

{%- if postgresql.ssl_enabled == True %}

/opt/salt/scripts/create_embeddeddb_certificate.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: postgres
    - mode: 750
    - source: salt://postgresql/scripts/create_embeddeddb_certificate.sh
    - template: jinja
    - context:
        postgres_directory: {{ postgres_directory }}

create_embeddeddb_certificate:
  cmd.run:
    - name: /opt/salt/scripts/create_embeddeddb_certificate.sh
    - require:
      - file: /opt/salt/scripts/create_embeddeddb_certificate.sh
    - unless: test -f {{ postgres_directory }}/certs/postgres.cert

{%- endif %}

{%- endif %}

{%- if salt['pillar.get']('postgres:postgres_version', '10') | int == 11 %}
include:
  - postgresql.pg11
{%- endif %}

ensure-postgres-stopped-before-initdb:
  service.dead:
    - name: postgresql
    - unless: grep -q UTF-8 {{ postgres_directory }}/initdb.log && test -f {{ postgres_directory }}/data/PG_VERSION

init-db-with-utf8:
  cmd.run:
    - name: rm -rf {{ postgres_directory }}/data/* && runuser -l postgres -s /bin/bash sh -c 'initdb --locale=en_US.UTF-8 {{ postgres_directory }}/data > {{ postgres_directory }}/initdb.log' && rm -f {{ postgres_log_directory }}/pgsql_listen_address_configured
    - unless: grep -q UTF-8 {{ postgres_directory }}/initdb.log && test -f {{ postgres_directory }}/data/PG_VERSION
    - failhard: True

{%- if postgres_data_on_attached_disk %}

change-db-location:
  file.replace:
    - name: {{ unitFile }}
    - pattern: "Environment=PGDATA=.*"
    - repl: Environment=PGDATA={{ postgres_directory }}/data
    - unless: grep "Environment=PGDATA={{ postgres_directory }}/data" {{ unitFile }}

systemctl-reload-on-pg-unit-change:
  cmd.run:
    - name: systemctl --system daemon-reload
    - onchanges:
        - file: change-db-location
{%- endif %}

start-postgresql:
  service.running:
    - enable: True
{%- if postgres_data_on_attached_disk %}
    - watch:
        - file: change-db-location
    - require:
        - cmd: systemctl-reload-on-pg-unit-change
{%- endif %}
    - name: postgresql

/opt/salt/scripts/conf_pgsql_listen_address.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: postgres
    - mode: 750
    - source: salt://postgresql/scripts/conf_pgsql_listen_address.sh

{{ postgres_scripts_executed_directory }}/pgsql_listen_address_configured:
  file.rename:
    - makedirs: True
    - source: {{ postgres_log_directory }}/pgsql_listen_address_configured
    - unless: test -f {{ postgres_scripts_executed_directory }}/pgsql_listen_address_configured

configure-listen-address:
  cmd.run:
    - name: runuser -l postgres -s /bin/bash -c '/opt/salt/scripts/conf_pgsql_listen_address.sh' && echo $(date +%Y-%m-%d:%H:%M:%S) >> {{ postgres_scripts_executed_directory }}/pgsql_listen_address_configured
    - require:
      - file: /opt/salt/scripts/conf_pgsql_listen_address.sh
      - service: start-postgresql
    - unless: test -f {{ postgres_scripts_executed_directory }}/pgsql_listen_address_configured

/opt/salt/scripts/conf_pgsql_max_connections.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: postgres
    - mode: 750
    - source: salt://postgresql/scripts/conf_pgsql_max_connections.sh

{{ postgres_scripts_executed_directory }}/pgsql_max_connections_configured:
  file.rename:
    - makedirs: True
    - source: {{ postgres_log_directory }}/pgsql_max_connections_configured
    - unless: test -f {{ postgres_scripts_executed_directory }}/pgsql_max_connections_configured

configure-max-connections:
  cmd.run:
    - name: runuser -l postgres -s /bin/bash -c '/opt/salt/scripts/conf_pgsql_max_connections.sh' && echo $(date +%Y-%m-%d:%H:%M:%S) >> {{ postgres_scripts_executed_directory }}/pgsql_max_connections_configured
    - require:
      - file: /opt/salt/scripts/conf_pgsql_max_connections.sh
      - service: start-postgresql
    - unless: test -f {{ postgres_scripts_executed_directory }}/pgsql_max_connections_configured

{%- if postgres_data_on_attached_disk and postgresql.ssl_enabled == True %}

/opt/salt/scripts/conf_pgsql_ssl.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: postgres
    - mode: 750
    - source: salt://postgresql/scripts/conf_pgsql_ssl.sh
    - template: jinja
    - context:
        postgres_directory: {{ postgres_directory }}

{{ postgres_scripts_executed_directory }}/pgsql_ssl_configured:
  file.rename:
    - makedirs: True
    - source: {{ postgres_log_directory }}/pgsql_ssl_configured
    - unless: test -f {{ postgres_scripts_executed_directory }}/pgsql_ssl_configured

configure-ssl:
  cmd.run:
    - name: runuser -l postgres -s /bin/bash -c '/opt/salt/scripts/conf_pgsql_ssl.sh' && echo $(date +%Y-%m-%d:%H:%M:%S) >> {{ postgres_scripts_executed_directory }}/pgsql_ssl_configured
    - require:
      - file: /opt/salt/scripts/conf_pgsql_ssl.sh
      - service: start-postgresql
    - unless: test -f {{ postgres_scripts_executed_directory }}/pgsql_ssl_configured

{%- endif %}

/opt/salt/scripts/init_db.sh:
  file.managed:
    - makedirs: True
    - require:
      - cmd: configure-listen-address
      - cmd: configure-max-connections
{%- if postgres_data_on_attached_disk and postgresql.ssl_enabled == True %}
      - cmd: configure-ssl
{%- endif %}
    - mode: 750
    - user: root
    - group: postgres
    - source: salt://postgresql/scripts/init_db.sh
    - template: jinja

{{ postgres_scripts_executed_directory }}/init-services-db-executed:
  file.rename:
    - makedirs: True
    - source: {{ postgres_log_directory }}/init-services-db-executed
    - unless: test -f {{ postgres_scripts_executed_directory }}/init-services-db-executed

init-services-db:
  cmd.run:
    - name: runuser -l postgres -s /bin/bash -c '/opt/salt/scripts/init_db.sh' && echo $(date +%Y-%m-%d:%H:%M:%S) >> {{ postgres_scripts_executed_directory }}/init-services-db-executed
    - unless: test -f {{ postgres_scripts_executed_directory }}/init-services-db-executed
    - require:
      - file: /opt/salt/scripts/init_db.sh
{%- if postgres_data_on_attached_disk and postgresql.ssl_enabled == True %}
      - file: {{ postgresql.root_certs_file }}
{%- endif %}
      - cmd: configure-listen-address
      - cmd: configure-max-connections
{%- if postgres_data_on_attached_disk and postgresql.ssl_enabled == True %}
      - cmd: configure-ssl
{%- endif %}

restart-pgsql-if-reconfigured:
  service.running:
    - name: postgresql
    - watch:
      - cmd: configure-listen-address
      - cmd: configure-max-connections
{%- if postgres_data_on_attached_disk and postgresql.ssl_enabled == True %}
      - cmd: configure-ssl
{%- endif %}
      - cmd: init-services-db

{% endif %}
