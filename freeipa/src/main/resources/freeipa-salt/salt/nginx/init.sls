/etc/nginx/sites-enabled/ssl.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl.conf
    - template: jinja

/etc/nginx/sites-enabled/ssl-locations.d/freeipa.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/freeipa.conf
    - template: jinja

/etc/nginx/sites-enabled/ssl-locations.d/saltapi.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/saltapi.conf

/etc/nginx/sites-enabled/ssl-locations.d/saltboot.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/saltboot.conf

/etc/nginx/sites-enabled/ssl-locations.d/freeipahealthcheck.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/freeipahealthcheck.conf
    - template: jinja

restart_nginx_after_ssl_reconfig:
  service.running:
    - name: nginx
    - enable: True
    - watch:
      - file: /etc/nginx/sites-enabled/ssl.conf
