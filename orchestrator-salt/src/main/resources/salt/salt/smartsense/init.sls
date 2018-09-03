{%- from 'ambari/settings.sls' import ambari with context %}

smartsense-hst:
  pkg.installed: []

update-smartsense:
  pkg.installed:
    - sources:
      - smartsense-hst: {{ pillar['smartsense_update_pkg'] }}
    - unless: test -f /usr/{{ ambari.stack_type }}/share/hst/ambari-service/SMARTSENSE/configuration/product-info.xml

upgrade-smartsense-ambari-service:
    file.copy:
    - name:  /var/lib/ambari-server/resources/stacks/HDP/2.1/services/SMARTSENSE
    - force: True
    - mode: 755
    - source: /usr/{{ ambari.stack_type }}/share/hst/ambari-service/SMARTSENSE
    - unless: test -f /var/lib/ambari-server/resources/stacks/HDP/2.1/services/SMARTSENSE/configuration/product-info.xml

/etc/hst/conf/hst-gateway.ini:
  file.managed:
    - makedirs: True
    - source: salt://smartsense/gateway/hst-gateway.ini
    - mode: 755
    - template: jinja
    - context:
        smartsense_upload: {{ salt['pillar.get']('smartsense_upload') }}
    - watch:
      - pkg: smartsense-hst

disable-hst-gateway:
  service.disabled:
    - name: hst-gateway

disable-hst:
  service.disabled:
    - name: hst
