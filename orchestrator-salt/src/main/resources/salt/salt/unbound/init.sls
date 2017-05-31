{%- from 'consul/settings.sls' import consul with context %}

/etc/unbound/conf.d/01-consul.conf:
  file.managed:
    - makedirs: True
    - source: salt://unbound/config/01-consul.conf
    - template: jinja
    - context:
        consul_server_address: {{ consul.server }}

/etc/unbound/conf.d/00-cluster.conf:
  file.managed:
    - makedirs: True
    - source: salt://unbound/config/00-cluster.conf
    - template: jinja

/etc/dhcp/dhclient.d/google_hostname.sh:
  file.managed:
    - makedirs: True
    - source: salt://unbound/dhcp/google_hostname.sh
    - mode: 744

/etc/unbound/conf.d/98-default.conf:
  file.managed:
    - makedirs: True
    - source: salt://unbound/config/98-default.conf
    - template: jinja

reload_unbound:
  cmd.run:
    - name: pkill -HUP unbound
    - watch:
      - file: /etc/unbound/conf.d/01-consul.conf
      - file: /etc/unbound/conf.d/00-cluster.conf
      - file: /etc/unbound/conf.d/98-default.conf