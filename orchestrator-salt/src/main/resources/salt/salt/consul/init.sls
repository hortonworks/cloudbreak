{%- from 'consul/settings.sls' import consul with context %}

#mycommand:
#  cmd.run:
#    - name: ls -l / >> /tmp/foo
#    - creates: /tmp/foo

/etc/cloudbreak/consul/consul.json:
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


/etc/init.d/consul:
  file.managed:
    - source: salt://consul/scripts/consul
    - mode: 755

/etc/systemd/system/consul.service:
  file.managed:
    - source: salt://consul/systemd/consul.service

start-consul:
  module.wait:
    - name: service.systemctl_reload
    - watch:
      - file: /etc/systemd/system/consul.service
  service.running:
    - enable: True
    - name: consul
    - watch:
       - file: /etc/systemd/system/consul.service