stop-ambari-server:
  service.dead:
   - name: ambari-server

reset-ambari:
  cmd.run:
   - name: ambari-server reset --silent

start-ambari-server:
  service.running:
    - enable: True
    - name: ambari-server