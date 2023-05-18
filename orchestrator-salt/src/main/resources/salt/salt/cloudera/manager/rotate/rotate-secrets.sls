{% set cm_db_rotation = salt['pillar.get']('postgres-rotation:clouderamanager') %}

rotate-db-username:
  file.replace:
    - name: /etc/cloudera-scm-server/db.properties
    - pattern: "^com.cloudera.cmf.db.user=.*"
    - repl: "com.cloudera.cmf.db.user={{ cm_db_rotation.newUser }}"
    - append_if_not_found: True

rotate-db-password:
  file.replace:
    - name: /etc/cloudera-scm-server/db.properties
    - pattern: "^com.cloudera.cmf.db.password=.*"
    - repl: "com.cloudera.cmf.db.password={{ cm_db_rotation.newPassword }}"
    - append_if_not_found: True