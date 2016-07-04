smartsense-hst:
  pkg.installed: []

/etc/hst/conf/hst-gateway.ini:
  file.managed:
    - makedirs: True
    - source: salt://smartsense/gateway/hst-gateway.ini
    - mode: 755
    - watch:
      - pkg: smartsense-hst

hst-gateway:
  service.disabled:
    - name: hst-gateway

hst:
  service.enabled:
    - name: hst
