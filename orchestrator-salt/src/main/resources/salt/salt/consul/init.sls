{%- from 'consul/settings.sls' import consul with context %}

/etc/consul.conf:
  file.managed:
    - makedirs: True
    - source: salt://consul/config/consul.json
    - template: jinja
    - context:
        advertise_addr: {{ consul.advertise_addr }}
        is_server: {{ consul.is_server }}
        node_name: {{ consul.node_name }}
        recursors: {{ consul.recursors }}
        bootstrap_expect: {{ consul.bootstrap_expect }}
        retry_join: {{ consul.retry_join }}


start-consul:
  service.running:
    - enable: True
    - name: consul_agent
