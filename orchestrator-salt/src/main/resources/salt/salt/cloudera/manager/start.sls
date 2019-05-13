{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}

init-cloudera-manager-db:
  cmd.run:
    - name: /opt/cloudera/cm/schema/scm_prepare_database.sh -h {{ cloudera_manager.cloudera_manager_database.host }} {{ cloudera_manager.cloudera_manager_database.subprotocol }} {{ cloudera_manager.cloudera_manager_database.databaseName }} {{ cloudera_manager.cloudera_manager_database.connectionUserName }} {{ cloudera_manager.cloudera_manager_database.connectionPassword }} && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/init-cloudera-manager-db-executed
    - unless: test -f /var/log/init-cloudera-manager-db-executed

start_server:
  service.running:
    - enable: True
    - name: cloudera-scm-server
