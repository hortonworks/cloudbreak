{%- from 'smartsense/settings.sls' import ambari with context %}

/opt/smartsense/smartsense-upgrade.sh:
  file.managed:
    - makedirs: True
    - source: salt://smartsense/scripts/smartsense-upgrade.sh
    - template: jinja
    - context:
      ambari: {{ ambari }}
    - mode: 744

upgrade_hst_ambari:
  cmd.run:
    - name: /opt/smartsense/smartsense-upgrade.sh 2>&1 | tee -a /var/log/ambari-upgrade/smartsense-upgrade.log && exit ${PIPESTATUS[0]}
    - unless: cat /var/smartsense-upgrade.history | grep '{{ ambari.version }}' &>/dev/null