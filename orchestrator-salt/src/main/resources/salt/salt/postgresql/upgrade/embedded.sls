
{% set postgres_directory = '/dbfs/pgsql11' %}
{% set postgres_log_directory = '/dbfs/pgsql11/log' %}

stop-postgresql:
  service.dead:
    - name: postgresql

{% set command = 'systemctl show -p FragmentPath postgresql-11' %}
{% set unitFile = salt['cmd.run'](command) | replace("FragmentPath=","") %}

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

/dbfs/pgsql10:
  file.copy:
    - source: /usr/pgsql-10
    - user: postgres
    - group: postgres

change-db-location:
  file.replace:
    - name: {{ unitFile }}
    - pattern: "Environment=PGDATA=.*"
    - repl: Environment=PGDATA={{ postgres_directory }}/data
    - unless: grep "Environment=PGDATA={{ postgres_directory }}/data" {{ unitFile }}

init-db-with-utf8:
  cmd.run:
    - name: rm -rf {{ postgres_directory }}/data && runuser -l postgres -s /bin/bash sh -c '/usr/pgsql-11/bin/initdb --locale=en_US.UTF-8 {{ postgres_directory }}/data > {{ postgres_directory }}/initdb.log'
    - unless: grep -q UTF-8 {{ postgres_directory }}/initdb.log

upgrade_postgresql_db:
  cmd.run:
    - name: runuser -l postgres -s /bin/bash sh -c '/usr/pgsql-11/bin/pg_upgrade -b /dbfs/pgsql10/bin -B /usr/pgsql-11/bin -d /dbfs/pgsql/data -D {{ postgres_directory }}/data'

{{ postgres_directory }}/data/postgresql.conf:
  file.copy:
    - source: /dbfs/pgsql/data/postgresql.conf
    - force: True
    - user: postgres
    - group: postgres

{{ postgres_directory }}/data/pg_hba.conf:
  file.copy:
    - source: /dbfs/pgsql/data/pg_hba.conf
    - force: True
    - user: postgres
    - group: postgres

start-postgresql11:
  service.running:
    - name: postgresql-11
