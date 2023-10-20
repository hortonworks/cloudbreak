{% set db_username = salt['pillar.get']('cloudera-manager:database:connectionUserName') %}

update-db-username:
  file.replace:
    - name: /etc/cloudera-scm-server/db.properties
    - pattern: "^com.cloudera.cmf.db.user=.*"
    - repl: "com.cloudera.cmf.db.user={{ db_username }}"
    - append_if_not_found: True
