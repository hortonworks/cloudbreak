{% set gateway = salt['pillar.get']('gateway') %}

/etc/nginx/sites-enabled/ssl.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl.conf
    - replace: False

/etc/nginx/sites-enabled/ssl-locations.d/consul.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/consul.conf

/etc/nginx/sites-enabled/ssl-locations.d/prometheus.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/prometheus.conf

/etc/nginx/sites-enabled/ssl-locations.d/saltapi.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/saltapi.conf

/etc/nginx/sites-enabled/ssl-locations.d/saltboot.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/saltboot.conf

{% if "ambari_server" in grains.get('roles', []) %}

/etc/nginx/sites-enabled/ssl-locations.d/ambari.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/ambari.conf

{% endif %}

{% if "manager_server" in grains.get('roles', []) %}

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

/etc/nginx/sites-enabled/ssl-locations.d/clouderamanager.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/clouderamanager.conf

{% endif %}

# when gateway is defined, we do NOT config user facing cert and ssl for port 443, because services are available through knox on 8443
# we still config ssl for port 9443 (for internal communication with ambari)
{% if gateway.path is defined and gateway.path is not none and "manager_server" not in grains.get('roles', []) %}

restart_nginx_after_ssl_reconfig:
  service.running:
    - name: nginx
    - enable: True
    - watch:
      - file: /etc/nginx/sites-enabled/ssl.conf

# when gateway is NOT defined, we config user facing cert and ssl, because services are available through nginx on 443
{% else %}

add_user_facing_cert_script:
  file.managed:
    - name: /opt/salt/scripts/create-user-facing-cert.sh
    - source: salt://nginx/scripts/create-user-facing-cert.sh
    - makedirs: True
    - template: jinja
    - mode: 755

/etc/certs-user-facing:
  file.directory:
    - makedirs: True

generate_user_facing_cert:
  cmd.run:
    - name: /opt/salt/scripts/create-user-facing-cert.sh 2>&1 | tee -a /var/log/generate-user-facing-cert.log && exit ${PIPESTATUS[0]}
    - unless: test -f /etc/certs-user-facing/server.pem

/etc/nginx/sites-enabled/ssl-user-facing.conf:
  file.managed:
    - makedirs: True
  {% if "ambari_server" in grains.get('roles', []) %}
    - source: salt://nginx/conf/ambari-ssl-user-facing.conf
  {% elif "manager_server" in grains.get('roles', []) %}
    - source: salt://nginx/conf/clouderamanager-ssl-user-facing.conf
  {% endif %}
    - replace: False

restart_nginx_after_ssl_reconfig_with_user_facing:
  service.running:
    - name: nginx
    - enable: True
    - watch:
      - file: /etc/nginx/sites-enabled/ssl.conf
      - file: /etc/nginx/sites-enabled/ssl-user-facing.conf

{% endif %}