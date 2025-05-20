pgsql-ld-conf:
  alternatives.set:
    - path: /usr/pgsql-17/share/postgresql-17-libs.conf

pgsql-psql:
  alternatives.set:
    - path: /usr/pgsql-17/bin/psql

pgsql-clusterdb:
  alternatives.set:
    - path: /usr/pgsql-17/bin/clusterdb

pgsql-createdb:
  alternatives.set:
    - path: /usr/pgsql-17/bin/createdb

pgsql-createuser:
  alternatives.set:
    - path: /usr/pgsql-17/bin/createuser

pgsql-dropdb:
  alternatives.set:
    - path: /usr/pgsql-17/bin/dropdb

pgsql-dropuser:
  alternatives.set:
    - path: /usr/pgsql-17/bin/dropuser

pgsql-pg_basebackup:
  alternatives.set:
    - path: /usr/pgsql-17/bin/pg_basebackup

pgsql-pg_dump:
  alternatives.set:
    - path: /usr/pgsql-17/bin/pg_dump

pgsql-pg_dumpall:
  alternatives.set:
    - path: /usr/pgsql-17/bin/pg_dumpall

pgsql-pg_restore:
  alternatives.set:
    - path: /usr/pgsql-17/bin/pg_restore

pgsql-reindexdb:
  alternatives.set:
    - path: /usr/pgsql-17/bin/reindexdb

pgsql-vacuumdb:
  alternatives.set:
    - path: /usr/pgsql-17/bin/vacuumdb

pgsql-clusterdbman:
  alternatives.set:
    - path: /usr/pgsql-17/share/man/man1/clusterdb.1

pgsql-createdbman:
  alternatives.set:
    - path: /usr/pgsql-17/share/man/man1/createdb.1

pgsql-createuserman:
  alternatives.set:
    - path: /usr/pgsql-17/share/man/man1/createuser.1

pgsql-dropdbman:
  alternatives.set:
    - path: /usr/pgsql-17/share/man/man1/dropdb.1

pgsql-dropuserman:
  alternatives.set:
    - path: /usr/pgsql-17/share/man/man1/dropuser.1

pgsql-pg_basebackupman:
  alternatives.set:
    - path: /usr/pgsql-17/share/man/man1/pg_basebackup.1

pgsql-pg_dumpman:
  alternatives.set:
    - path: /usr/pgsql-17/share/man/man1/pg_dump.1

pgsql-pg_dumpallman:
  alternatives.set:
    - path: /usr/pgsql-17/share/man/man1/pg_dumpall.1

pgsql-pg_restoreman:
  alternatives.set:
    - path: /usr/pgsql-17/share/man/man1/pg_restore.1

pgsql-psqlman:
  alternatives.set:
    - path: /usr/pgsql-17/share/man/man1/psql.1

pgsql-reindexdbman:
  alternatives.set:
    - path: /usr/pgsql-17/share/man/man1/reindexdb.1

pgsql-vacuumdbman:
  alternatives.set:
    - path: /usr/pgsql-17/share/man/man1/vacuumdb.1

/bin/initdb:
  file.symlink:
    - target: /usr/pgsql-17/bin/initdb
    - force: True