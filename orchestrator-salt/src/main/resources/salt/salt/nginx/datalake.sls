/etc/nginx/sites-enabled/datalake.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/datalake.conf
    - template: jinja

restart_nginx_after_datalake_config:
  service.running:
    - name: nginx
    - enable: True
    - watch:
      - file: /etc/nginx/sites-enabled/datalake.conf