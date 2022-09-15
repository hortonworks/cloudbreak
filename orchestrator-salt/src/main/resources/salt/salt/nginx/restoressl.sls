restore_last_ssl_conf:
  module.run:
    - name: file.restore_backup
    - path: /etc/nginx/sites-enabled/ssl.conf
    - backup_id: 0

restart_nginx_after_ssl_restore:
  service.running:
    - name: nginx
    - enable: True
    - failhard: True
    - watch:
      - module: restore_last_ssl_conf
