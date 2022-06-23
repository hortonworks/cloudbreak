{%- from 'metadata/settings.sls' import metadata with context %}

{% if not salt['file.file_exists']('/usr/pgsql-11/bin/psql') %}
include:
  - postgresql.repo.pg11

install-centos-scl-rh:
  pkg.installed:
    - failhard: True
    - name: centos-release-scl-rh

install-postgres11:
  pkg.installed:
    - failhard: True
    - pkgs:
        - postgresql11-server
        - postgresql-jdbc
        - postgresql11
        - postgresql11-contrib
        - postgresql11-docs
        - postgresql11-devel
{% endif %}

pgsql-ld-conf:
  alternatives.set:
    - path: /usr/pgsql-11/share/postgresql-11-libs.conf

pgsql-psql:
  alternatives.set:
    - path: /usr/pgsql-11/bin/psql

pgsql-clusterdb:
  alternatives.set:
    - path: /usr/pgsql-11/bin/clusterdb

pgsql-createdb:
  alternatives.set:
    - path: /usr/pgsql-11/bin/createdb

pgsql-createuser:
  alternatives.set:
    - path: /usr/pgsql-11/bin/createuser

pgsql-dropdb:
  alternatives.set:
    - path: /usr/pgsql-11/bin/dropdb

pgsql-dropuser:
  alternatives.set:
    - path: /usr/pgsql-11/bin/dropuser

pgsql-pg_basebackup:
  alternatives.set:
    - path: /usr/pgsql-11/bin/pg_basebackup

pgsql-pg_dump:
  alternatives.set:
    - path: /usr/pgsql-11/bin/pg_dump

pgsql-pg_dumpall:
  alternatives.set:
    - path: /usr/pgsql-11/bin/pg_dumpall

pgsql-pg_restore:
  alternatives.set:
    - path: /usr/pgsql-11/bin/pg_restore

pgsql-reindexdb:
  alternatives.set:
    - path: /usr/pgsql-11/bin/reindexdb

pgsql-vacuumdb:
  alternatives.set:
    - path: /usr/pgsql-11/bin/vacuumdb

pgsql-clusterdbman:
  alternatives.set:
    - path: /usr/pgsql-11/share/man/man1/clusterdb.1

pgsql-createdbman:
  alternatives.set:
    - path: /usr/pgsql-11/share/man/man1/createdb.1

pgsql-createuserman:
  alternatives.set:
    - path: /usr/pgsql-11/share/man/man1/createuser.1

pgsql-dropdbman:
  alternatives.set:
    - path: /usr/pgsql-11/share/man/man1/dropdb.1

pgsql-dropuserman:
  alternatives.set:
    - path: /usr/pgsql-11/share/man/man1/dropuser.1

pgsql-pg_basebackupman:
  alternatives.set:
    - path: /usr/pgsql-11/share/man/man1/pg_basebackup.1

pgsql-pg_dumpman:
  alternatives.set:
    - path: /usr/pgsql-11/share/man/man1/pg_dump.1

pgsql-pg_dumpallman:
  alternatives.set:
    - path: /usr/pgsql-11/share/man/man1/pg_dumpall.1

pgsql-pg_restoreman:
  alternatives.set:
    - path: /usr/pgsql-11/share/man/man1/pg_restore.1

pgsql-psqlman:
  alternatives.set:
    - path: /usr/pgsql-11/share/man/man1/psql.1

pgsql-reindexdbman:
  alternatives.set:
    - path: /usr/pgsql-11/share/man/man1/reindexdb.1

pgsql-vacuumdbman:
  alternatives.set:
    - path: /usr/pgsql-11/share/man/man1/vacuumdb.1

/bin/initdb:
  file.symlink:
    - target: /usr/pgsql-11/bin/initdb
    - force: True

/var/lib/pgsql/data:
  file.symlink:
    - target: /var/lib/pgsql/11/data
    - force: True

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
