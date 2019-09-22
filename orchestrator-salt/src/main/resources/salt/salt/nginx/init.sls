{% set gateway = salt['pillar.get']('gateway') %}

/etc/nginx/sites-enabled/ssl.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl.conf
    - replace: True

# when gateway is defined, we do NOT config user facing cert and ssl for port 443, because services are available through knox on 8443
# we still config ssl for port 9443 (for internal communication with ambari)
{% if gateway.path is defined and gateway.path is not none %}

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
    - source: salt://nginx/conf/ssl-user-facing.conf
    - replace: False

restart_nginx_after_ssl_reconfig_with_user_facing:
  service.running:
    - name: nginx
    - enable: True
    - watch:
      - file: /etc/nginx/sites-enabled/ssl.conf
      - file: /etc/nginx/sites-enabled/ssl-user-facing.conf

{% endif %}