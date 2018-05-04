start-postgresql:
  service.running:
    - enable: True
    - name: postgresql

/opt/salt/scripts/conf_pgsql_listen_address.sh:
  file.managed:
    - makedirs: True
    - mode: 755
    - source: salt://postgresql/scripts/conf_pgsql_listen_address.sh

configure-listen-address:
  cmd.run:
    - name: runuser -l postgres -c '/opt/salt/scripts/conf_pgsql_listen_address.sh' && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/pgsql_listen_address_configured
    - require:
      - file: /opt/salt/scripts/conf_pgsql_listen_address.sh
      - service: start-postgresql
    - unless: test -f /var/log/pgsql_listen_address_configured

/opt/salt/scripts/init_db.sh:
  file.managed:
    - makedirs: True
    - mode: 755
    - source: salt://postgresql/scripts/init_db.sh
    - template: jinja

init-services-db:
  cmd.run:
    - name: runuser -l postgres -c '/opt/salt/scripts/init_db.sh' && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/init-services-db-executed
    - unless: test -f /var/log/init-services-db-executed
    - require:
      - file: /opt/salt/scripts/init_db.sh

restart-pgsql-if-reconfigured:
  service.running:
    - enable: True
    - name: postgresql
    - watch:
      - cmd: configure-listen-address
      - cmd: init-services-db