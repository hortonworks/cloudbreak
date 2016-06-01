smartsense-hst:
  pkg.installed: []

patch:
  pkg.installed: []

/etc/smartsense/conf/smartsense-agent-patch.sh:
  file.managed:
    - makedirs: True
    - source: salt://smartsense/scripts/smartsense-agent-patch.sh
    - mode: 755
    - watch:
      - pkg: smartsense-hst
      - pkg: patch

execute-patch:
  cmd.run:
    - name: /etc/smartsense/conf/smartsense-agent-patch.sh
    - watch:
      - file: /etc/smartsense/conf/smartsense-agent-patch.sh