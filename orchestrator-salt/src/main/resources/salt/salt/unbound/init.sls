{%- from 'consul/settings.sls' import consul with context %}

unbound:
  pkg:
    - installed
  service.running:
    - watch:
      - pkg: unbound
      - file: /etc/unbound/conf.d/01-consul.conf
      - file: /etc/unbound/conf.d/00-cluster.conf

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