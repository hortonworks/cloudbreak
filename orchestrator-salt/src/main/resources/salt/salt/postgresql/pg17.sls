{%- from 'metadata/settings.sls' import metadata with context %}

{% set postgres_directory = salt['pillar.get']('postgres:postgres_directory') %}
{% set postgres_data_on_attached_disk = salt['pillar.get']('postgres:postgres_data_on_attached_disk', 'False') %}

include:
{% if not salt['file.file_exists']('/usr/pgsql-17/bin/psql') %}
  - postgresql.pg-install
{% endif %}
  - postgresql.pg-alternatives

{% for version in ['10', '11', '14'] %}
{% if salt['file.file_exists']('/usr/lib/systemd/system/postgresql-' +  version + '.service') %}
remove-pg{{ version }}-alias:
  file.replace:
    - name: /usr/lib/systemd/system/postgresql-{{ version }}.service
    - pattern: "Alias=postgresql.service"
    - repl: ""

disable-postgresql-{{ version }}:
  service.dead:
    - enable: False
    - name: postgresql-{{ version }}
{% endif %}
{% endfor %}

/var/lib/pgsql/data:
  file.symlink:
    - target: /var/lib/pgsql/17/data
    - force: True
    - failhard: True
    - user: postgres
    - group: postgres
    - mode: 700
    - makedirs: True

change-db-location-17:
  file.replace:
    - name: /usr/lib/systemd/system/postgresql-17.service
    - pattern: "Environment=PGDATA=.*"
    - repl: Environment=PGDATA={{ postgres_directory }}/data
    - unless: grep "Environment=PGDATA={{ postgres_directory }}/data" /usr/lib/systemd/system/postgresql-17.service

postgresql-systemd-link:
  file.replace:
    - name: /usr/lib/systemd/system/postgresql-17.service
    - pattern: "\\[Install\\]"
    - repl: "[Install]\nAlias=postgresql.service"
    - unless: cat /usr/lib/systemd/system/postgresql-17.service | grep postgresql.service

{% if metadata.platform == 'YARN' %}  # systemctl reenable does not work on ycloud so we create the symlink manually
create-postgres-service-link:
  cmd.run:
    - name: ln -sf /usr/lib/systemd/system/postgresql-17.service /usr/lib/systemd/system/postgresql.service && systemctl disable postgresql-17 && systemctl enable postgresql
{% else %}
reenable-postgres:
  cmd.run:
    - name: systemctl reenable postgresql-17.service
{% endif %}

systemctl-reload-on-pg17-unit-change:
  cmd.run:
    - name: systemctl --system daemon-reload
    - onchanges:
        - file: change-db-location-17
