{%- from 'consul/settings.sls' import consul with context %}

/etc/consul.conf:
  file.managed:
    - makedirs: True
    - source: salt://consul/config/consul.json
    - template: jinja
    - context:
        is_server: {{ consul.is_server }}
        advertise_addr: {{ consul.advertise_addr }}
        node_name: {{ consul.node_name }}
        bootstrap_expect: {{ consul.bootstrap_expect }}
        retry_join: {{ consul.retry_join }}

/opt/consul/consul_cleanup.sh:
  file.managed:
    - makedirs: True
    - source: salt://consul/scripts/consul_cleanup.sh
    - mode: 755

remove_failing_nodes:
  cmd.run:
    - name: /opt/consul/consul_cleanup.sh
    - watch:
      - file: /etc/consul.conf

consul:
  service.running:
    - enable: True

{% if consul.is_server == True %}
consul-template:
  service.running:
    - enable: True
{% endif %}