/etc/nginx/sites-enabled/datalake.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/datalake.conf
    - template: jinja

reload_nginx:
  cmd.run:
    - name: pkill -HUP nginx

nginx:
  service.running:
    - enable: True
    - watch:
      - file: /etc/nginx/sites-enabled/datalake.conf