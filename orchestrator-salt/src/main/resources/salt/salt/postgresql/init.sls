{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

{% if 'None' != configure_remote_db %}

/opt/salt/scripts/init_db_remote.sh:
  file.managed:
    - makedirs: True
    - mode: 755
    - source: salt://postgresql/scripts/init_db_remote.sh
    - template: jinja

init-services-db-remote:
  cmd.run:
    - name: runuser -l postgres -c '/opt/salt/scripts/init_db_remote.sh' && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/init-services-db-remote-executed
    - unless: test -f /var/log/init-services-db-remote-executed
    - require:
      - file: /opt/salt/scripts/init_db_remote.sh

{%- else %}

init-db-with-utf8:
  cmd.run:
    - name: rm -rf /var/lib/pgsql/data && runuser -l postgres sh -c 'initdb --locale=en_US.UTF-8 /var/lib/pgsql/data > /var/lib/pgsql/initdb.log' && rm /var/log/pgsql_listen_address_configured
    - unless: grep -q UTF-8 /var/lib/pgsql/initdb.log

start-postgresql:
  service.running:
    - enable: True
    - require:
      - cmd: init-db-with-utf8
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
    - require:
      - cmd: configure-listen-address
    - mode: 755
    - source: salt://postgresql/scripts/init_db.sh
    - template: jinja

init-services-db:
  cmd.run:
    - name: runuser -l postgres -c '/opt/salt/scripts/init_db.sh' && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/init-services-db-executed
    - unless: test -f /var/log/init-services-db-executed
    - require:
      - file: /opt/salt/scripts/init_db.sh
      - cmd: configure-listen-address

{% if not salt['file.directory_exists']('/yarn-private') %}  # FIXME (BUG-92637): must be disabled for YCloud

restart-pgsql-if-reconfigured:
  service.running:
    - enable: True
    - name: postgresql
    - watch:
      - cmd: configure-listen-address
      - cmd: init-services-db

{% else %}

restart-postgresql:
  cmd.run:
    - name: netstat -tlpn |grep -q "0 0.0.0.0:5432" && service postgresql reload || service postgresql restart && while ! netstat -tlpn |grep -i "0 0.0.0.0:5432" &> /dev/null; do echo "waiting for postgres"; sleep 1; done
    - watch:
      - cmd: configure-listen-address
      - cmd: init-services-db

{% endif %}

{% endif %}