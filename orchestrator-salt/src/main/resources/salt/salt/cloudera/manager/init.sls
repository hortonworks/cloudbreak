{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}


install-cloudera-manager-server:
  pkg.installed:
    - pkgs:
      - cloudera-manager-daemons
      - cloudera-manager-agent
      - cloudera-manager-server

init-cloudera-manager-db:
  cmd.run:
    - name: /opt/cloudera/cm/schema/scm_prepare_database.sh -h {{ cloudera_manager.cloudera_manager_database.host }} {{ cloudera_manager.cloudera_manager_database.subprotocol }} {{ cloudera_manager.cloudera_manager_database.databaseName }} {{ cloudera_manager.cloudera_manager_database.connectionUserName }} {{ cloudera_manager.cloudera_manager_database.connectionPassword }} && echo $(date +%Y-%m-%d:%H:%M:%S) >>  /var/import-certificate_success
    - unless: test -f /var/log/init-cloudera-manager-db-executed

start_server:
  service.running:
    - enable: True
    - name: cloudera-scm-server

update_nginx_ssl_conf:
  file.replace:
    - name: /etc/nginx/sites-enabled/ssl.conf
    - pattern: "ambari"
    - repl: "clouderamanager"
    - unless: cat /etc/nginx/sites-enabled/ssl.conf | clouderamanager

update_nginx_ssl_conf_manager_session_id:
  file.replace:
    - name: /etc/nginx/sites-enabled/ssl.conf
    - pattern: "cookie_AMBARISESSIONID"
    - repl: "cookie_CLOUDERA_MANAGER_SESSIONID"
    - unless: cat /etc/nginx/sites-enabled/ssl.conf | grep cookie_CLOUDERA_MANAGER_SESSIONID

update_nginx_conf:
  file.replace:
    - name: /etc/nginx/nginx.conf
    - pattern: "ambari"
    - repl: "clouderamanager"
    - unless: cat /etc/nginx/nginx.conf | grep  clouderamanager

update_nginx_conf_manager_port:
  file.replace:
    - name: /etc/nginx/nginx.conf
    - pattern: "127.0.0.1:8080"
    - repl: "127.0.0.1:7180"
    - unless: cat /etc/nginx/nginx.conf | grep 127.0.0.1:7180

restart_nginx_after_cloudera_manager_ssl_reconfig:
  service.running:
    - name: nginx
    - enable: True
    - watch:
      - file: /etc/nginx/sites-enabled/ssl.conf