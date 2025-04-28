/etc/nginx/sites-enabled/ssl.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl.conf
    - template: jinja
    - backup: minion

/etc/nginx/sites-enabled/ssl-locations.d/freeipa.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/freeipa.conf
    - template: jinja

/etc/nginx/sites-enabled/ssl-locations.d/saltapi.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/saltapi.conf
    - template: jinja

update_saltboot_port_in_nginx_conf:
  file.replace:
    - name: /etc/nginx/nginx.conf
    - pattern: "server 127.0.0.1:7070;"
    - repl: "server 127.0.0.1:7071;"
    - onlyif: grep -qx "Environment='SALTBOOT_HTTPS_ENABLED=true'" /etc/systemd/system/salt-bootstrap.service

/etc/nginx/sites-enabled/ssl-locations.d/saltboot.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/saltboot.conf
    - template: jinja

/etc/nginx/sites-enabled/ssl-locations.d/freeipahealthcheck.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/freeipahealthcheck.conf
    - template: jinja

{% if salt['file.file_exists']('/cdp/ipaldapagent/cdp-freeipa-ldapagent') %}
/etc/nginx/sites-enabled/ssl-locations.d/ldapagent.conf:
  file.managed:
    - makedirs: True
    - source: salt://nginx/conf/ssl-locations.d/ldapagent.conf
    - template: jinja
{% endif %}

restart_nginx_after_ssl_reconfig:
  service.running:
    - name: nginx
    - enable: True
    - failhard: True
    - watch:
      - file: /etc/nginx/sites-enabled/ssl.conf
