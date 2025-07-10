{%- from 'metadata/settings.sls' import metadata with context %}

{% set postgres_directory = salt['pillar.get']('postgres:postgres_directory') %}
{% set postgres_data_on_attached_disk = salt['pillar.get']('postgres:postgres_data_on_attached_disk', 'False') %}


include:
{% if not salt['file.file_exists']('/usr/pgsql-11/bin/psql') %}
  - postgresql.pg11-install
{% endif %}
  - postgresql.pg11-alternatives


{% if salt['file.file_exists']('/usr/lib/systemd/system/postgresql-10.service') %}
remove-pg10-alias:
  file.replace:
    - name: /usr/lib/systemd/system/postgresql-10.service
    - pattern: "Alias=postgresql.service"
    - repl: ""

disable-postgresql-10:
  service.dead:
    - enable: False
    - name: postgresql-10
{% endif %}

/var/lib/pgsql/data:
  file.symlink:
    - target: /var/lib/pgsql/11/data
    - force: True
    - failhard: True
    - user: postgres
    - group: postgres
    - mode: 700
    - makedirs: True

change-db-location-11:
  file.replace:
    - name: /usr/lib/systemd/system/postgresql-11.service
    - pattern: "Environment=PGDATA=.*"
    - repl: Environment=PGDATA={{ postgres_directory }}/data
    - unless: grep "Environment=PGDATA={{ postgres_directory }}/data" /usr/lib/systemd/system/postgresql-11.service

postgresql-systemd-link:
  file.replace:
    - name: /usr/lib/systemd/system/postgresql-11.service
    - pattern: "\\[Install\\]"
    - repl: "[Install]\nAlias=postgresql.service"
    - unless: cat /usr/lib/systemd/system/postgresql-11.service | grep postgresql.service

{% if metadata.platform == 'YARN' %}  # systemctl reenable does not work on ycloud so we create the symlink manually
create-postgres-service-link:
  cmd.run:
    - name: ln -sf /usr/lib/systemd/system/postgresql-11.service /usr/lib/systemd/system/postgresql.service && systemctl disable postgresql-11 && systemctl enable postgresql
{% else %}
reenable-postgres:
  cmd.run:
    - name: systemctl reenable postgresql-11.service
{% endif %}

systemctl-reload-on-pg11-unit-change:
  cmd.run:
    - name: systemctl --system daemon-reload
    - onchanges:
        - file: change-db-location-11
