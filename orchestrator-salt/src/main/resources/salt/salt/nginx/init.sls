{% set gateway = salt['pillar.get']('gateway') %}

/etc/nginx/sites-enabled/ssl.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl.conf
    - template: jinja

/etc/nginx/sites-enabled/ssl-locations.d/consul.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/consul.conf

/etc/nginx/sites-enabled/ssl-locations.d/knox.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/knox.conf

/etc/nginx/sites-enabled/ssl-locations.d/prometheus.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/prometheus.conf

/etc/nginx/sites-enabled/ssl-locations.d/nodestatus.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/nodestatus.conf
    - template: jinja

/etc/nginx/sites-enabled/ssl-locations.d/saltapi.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/saltapi.conf
    - template: jinja

/etc/nginx/sites-enabled/ssl-locations.d/saltboot.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/saltboot.conf

{% if "manager_server" in grains.get('roles', []) %}
{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}

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
    - repl: "127.0.0.1:{{ cloudera_manager.communication.port }}"
    - unless: grep 127.0.0.1:{{ cloudera_manager.communication.port }} /etc/nginx/nginx.conf

/etc/nginx/sites-enabled/ssl-locations.d/clouderamanager.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/clouderamanager.conf
    - template: jinja
    - context:
      protocol: {{ cloudera_manager.communication.protocol }}
{% endif %}

/etc/certs-user-facing:
  file.directory:
    - makedirs: True

{% if gateway.userfacingcert_configured is defined and gateway.userfacingcert_configured == True %}

/etc/certs-user-facing/server-key.pem:
  file.managed:
    - contents_pillar: gateway:userfacingkey
    - makedirs: True
    - user: root
    - group: root
    - mode: 600
    - require_in:
      - service: restart_nginx_after_ssl_reconfig_with_user_facing

/etc/certs-user-facing/server.pem:
  file.managed:
    - contents_pillar: gateway:userfacingcert
    - makedirs: True
    - user: root
    - group: root
    - mode: 600
    - require_in:
      - service: restart_nginx_after_ssl_reconfig_with_user_facing

{% else %}

add_user_facing_cert_script:
  file.managed:
    - name: /opt/salt/scripts/create-user-facing-cert.sh
    - source: salt://nginx/scripts/create-user-facing-cert.sh
    - makedirs: True
    - template: jinja
    - mode: 755

generate_user_facing_cert:
  cmd.run:
    - name: /opt/salt/scripts/create-user-facing-cert.sh 2>&1 | tee -a /var/log/generate-user-facing-cert.log && exit ${PIPESTATUS[0]}
    - unless: test -f /etc/certs-user-facing/server.pem
    - require_in:
      - service: restart_nginx_after_ssl_reconfig_with_user_facing

{% endif %}

/etc/nginx/sites-enabled/ssl-user-facing.conf:
  file.managed:
    - makedirs: True
    {%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}
    - source: salt://nginx/conf/clouderamanager-ssl-user-facing.conf
    - template: jinja
    - context:
      protocol: {{ cloudera_manager.communication.protocol }}

restart_nginx_after_ssl_reconfig_with_user_facing:
  service.running:
    - name: nginx
    - enable: True
    - watch:
      - file: /etc/nginx/sites-enabled/ssl.conf
      - file: /etc/nginx/sites-enabled/ssl-user-facing.conf
      {% if gateway.userfacingcert_configured is defined and gateway.userfacingcert_configured == True %}
      - file: /etc/certs-user-facing/server-key.pem
      - file: /etc/certs-user-facing/server.pem
      {% endif %}
