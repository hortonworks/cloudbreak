smartsense-hst:
  pkg.installed: []

/etc/hst/conf/hst-gateway.ini:
  file.managed:
    - makedirs: True
    - source: salt://smartsense/gateway/hst-gateway.ini
    - mode: 755
    - watch:
      - pkg: smartsense-hst

disable-hst-gateway:
  cmd.run:
    - name: chkconfig hst-gateway off

enable-hst:
  cmd.run:
    - name: chkconfig hst on