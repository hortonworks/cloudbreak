{%- from 'postgresql/settings.sls' import postgresql with context %}
{% set original_postgres_version = salt['pillar.get']('postgres:upgrade:original_postgres_version', '10') %}
{% set original_postgres_directory = salt['pillar.get']('postgres:upgrade:original_postgres_directory', '/dbfs/pgsql') %}
{% set original_postgres_binaries = salt['pillar.get']('postgres:upgrade:original_postgres_binaries', '/dbfs/tmp/pgsql-10') %}

{% set new_postgres_version = salt['pillar.get']('postgres:upgrade:new_postgres_version', '11') %}
{% set new_postgres_directory = salt['pillar.get']('postgres:upgrade:new_postgres_directory', '/dbfs/pgsql-' + new_postgres_version) %}
{% set new_postgres_log_directory = salt['pillar.get']('postgres:upgrade:new_postgres_log_directory', '/dbfs/pgsql-' + new_postgres_version + '/log') %}
{% set new_postgres_binaries = salt['pillar.get']('postgres:upgrade:new_postgres_binaries', '/usr/pgsql-' + new_postgres_version) %}

{% if salt['file.file_exists']('/usr/pgsql-' + new_postgres_version + '/bin/psql') and salt['file.directory_exists'](original_postgres_binaries) %}

stop-postgresql:
  service.dead:
    - name: postgresql
    - enable: False

{% set command = 'systemctl show -p FragmentPath postgresql-' + new_postgres_version %}
{% set unitFile = salt['cmd.run'](command) | replace("FragmentPath=","") %}

{{ new_postgres_directory }}:
  file.directory:
    - user: postgres
    - group: postgres
    - mode: 700

{{ new_postgres_log_directory }}:
  file.directory:
    - user: root
    - group: root
    - mode: 755

init-db-with-utf8:
  cmd.run:
    - name: rm -rf {{ new_postgres_directory }}/data && runuser -l postgres -s /bin/bash sh -c '{{ new_postgres_binaries }}/bin/initdb --locale=en_US.UTF-8 {{ new_postgres_directory }}/data > {{ new_postgres_directory }}/initdb.log'
    - unless: grep -q UTF-8 {{ new_postgres_directory }}/initdb.log && test -f {{ new_postgres_directory }}/data/PG_VERSION
    - failhard: True

upgrade_postgresql_db:
  cmd.run:
    - name: runuser -l postgres -s /bin/bash sh -c '{{ new_postgres_binaries }}/bin/pg_upgrade -b {{ original_postgres_binaries }}/bin -B {{ new_postgres_binaries }}/bin -d {{ original_postgres_directory }}/data -D {{ new_postgres_directory }}/data > {{ new_postgres_directory }}/upgradedb.log'
    - failhard: True
    - require:
      - cmd: init-db-with-utf8

{{ new_postgres_directory }}/data/postgresql.conf:
  file.copy:
    - source: {{ original_postgres_directory }}/data/postgresql.conf
    - force: True
    - user: postgres
    - group: postgres
    - require:
      - cmd: upgrade_postgresql_db

{{ new_postgres_directory }}/data/pg_hba.conf:
  file.copy:
    - source: {{ original_postgres_directory }}/data/pg_hba.conf
    - force: True
    - user: postgres
    - group: postgres
    - require:
      - cmd: upgrade_postgresql_db

{{ new_postgres_directory }}/scripts:
  file.copy:
    - makedirs: True
    - source: {{ original_postgres_directory}}/scripts
    - force: True
    - require:
      - cmd: upgrade_postgresql_db

{%- if postgresql.ssl_enabled == True %}

{{ new_postgres_directory }}/certs:
  file.copy:
    - makedirs: True
    - source: {{ original_postgres_directory}}/certs
    - force: True
    - user: postgres
    - group: postgres
    - require:
      - cmd: upgrade_postgresql_db

{%- endif %}

remove_original_binaries:
  file.absent:
    - name: {{ original_postgres_binaries }}
    - require:
      - cmd: upgrade_postgresql_db

remove_original_data_directory:
  file.absent:
    - name: {{ original_postgres_directory }}
    - require:
      - cmd: upgrade_postgresql_db

{{ original_postgres_directory }}:
  file.rename:
    - makedirs: True
    - source: {{ new_postgres_directory }}
    - require:
      - cmd: upgrade_postgresql_db

change-db-location:
  file.replace:
    - name: {{ unitFile }}
    - pattern: "Environment=PGDATA=.*"
    - repl: Environment=PGDATA={{ original_postgres_directory }}/data
    - unless: grep "Environment=PGDATA={{ original_postgres_directory }}/data" {{ unitFile }}
    - require:
      - cmd: upgrade_postgresql_db

systemctl-reload-on-pg-unit-change:
  cmd.run:
    - name: systemctl --system daemon-reload
    - onchanges:
        - file: change-db-location

{# This only necessary because the postgres_directory is created by salt, which is unconfined as of right now,
   therefore the directory and its contents won't receive the correct labels by default. #}
apply-selinux-labels-to-pg-data-location:
  cmd.run:
    - name: restorecon -Rvi "$(echo '{{ new_postgres_directory }}' | sed -E 's#/pgsql/?$##')"

start-postgresql-{{ new_postgres_version }}:
  service.running:
    - name: postgresql-{{ new_postgres_version }}
    - enable: True
    - require:
      - cmd: upgrade_postgresql_db

include:
  - postgresql.pg{{ new_postgres_version }}

{% endif %}
