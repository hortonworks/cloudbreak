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

/etc/nginx/sites-enabled/ssl.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl.conf

restart_nginx_after_ssl_reconfig:
  service.running:
    - name: nginx
    - enable: True
    - watch:
      - file: /etc/nginx/sites-enabled/ssl.conf
      - file: /etc/nginx/sites-enabled/ssl-user-facing.conf
