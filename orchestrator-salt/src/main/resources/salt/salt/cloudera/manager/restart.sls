/var/lib/cloudera-scm-agent/agent-cert:
  file.directory:
    - user: root
    - group: root
    - mode: 755

stop-cloudera-scm-server:
  service.dead:
    - name: cloudera-scm-server

start-cloudera-scm-server:
  service.running:
    - name: cloudera-scm-server
