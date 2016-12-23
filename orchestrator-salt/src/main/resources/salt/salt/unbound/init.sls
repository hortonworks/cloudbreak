{%- from 'consul/settings.sls' import consul with context %}

/etc/unbound/conf.d/01-consul.conf:
  file.managed:
    - makedirs: True
    - source: salt://unbound/config/01-consul.conf
    - template: jinja
    - context:
        consul_server_address: {{ consul.server }}