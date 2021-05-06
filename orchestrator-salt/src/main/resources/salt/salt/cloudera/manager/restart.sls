stop-cloudera-scm-server:
  service.dead:
    - name: cloudera-scm-server

start-cloudera-scm-server:
  service.running:
    - name: cloudera-scm-server
