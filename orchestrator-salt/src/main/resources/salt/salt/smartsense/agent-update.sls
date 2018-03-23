install-smartsense-agent:
  pkg.installed:
   - name: smartsense-hst

update-smartsense-agent:
  pkg.installed:
    - sources:
      - smartsense-hst: {{ pillar['smartsense_update_pkg'] }}
    - unless: test -f /usr/hdp/share/hst/ambari-service/SMARTSENSE/configuration/product-info.xml

upgrade-smartsense-agent-ambari-agent-cache:
    file.copy:
    - name:  /var/lib/ambari-agent/cache/stacks/HDP/2.1/services/SMARTSENSE
    - force: True
    - mode: 755
    - source: /usr/hdp/share/hst/ambari-service/SMARTSENSE
    - unless: test -f /var/lib/ambari-agent/cache/stacks/HDP/2.1/services/SMARTSENSE/configuration/product-info.xml

disable-hst-gateway-on-agent:
  service.disabled:
    - name: hst-gateway

disable-hst-agent-update:
  service.disabled:
    - name: hst

reset-hst-agent:
    cmd.run:
      - name: hst reset-agent -q
