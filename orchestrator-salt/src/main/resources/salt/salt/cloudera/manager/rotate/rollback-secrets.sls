{% set cm_db_rotation = salt['pillar.get']('postgres-rotation:clouderamanager') %}

rollback-db-username:
  file.replace:
    - name: /etc/cloudera-scm-server/db.properties
    - pattern: "^com.cloudera.cmf.db.user=.*"
    - repl: "com.cloudera.cmf.db.user={{ cm_db_rotation.oldUser }}"
    - append_if_not_found: True

rollback-db-password:
  file.replace:
    - name: /etc/cloudera-scm-server/db.properties
    - pattern: "^com.cloudera.cmf.db.password=.*"
    - repl: "com.cloudera.cmf.db.password={{ cm_db_rotation.oldPassword }}"
    - append_if_not_found: True