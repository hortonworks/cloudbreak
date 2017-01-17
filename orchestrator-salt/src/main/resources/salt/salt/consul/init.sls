{%- from 'consul/settings.sls' import consul with context %}

/etc/consul.conf:
  file.managed:
    - makedirs: True
    - source: salt://consul/config/consul.json
    - template: jinja
    - context:
        is_server: {{ consul.is_server }}
        node_name: {{ consul.node_name }}
        bootstrap_expect: {{ consul.bootstrap_expect }}
        retry_join: {{ consul.retry_join }}

consul:
  service.running:
    - enable: True

{% if consul.is_server == True %}
consul-template:
  service.running:
    - enable: True
{% endif %}