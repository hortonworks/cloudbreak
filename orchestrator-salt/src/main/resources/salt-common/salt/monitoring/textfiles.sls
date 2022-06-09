{%- from 'monitoring/settings.sls' import monitoring with context %}

{%- if monitoring.enabled and monitoring.nodeExporterExists %}

/var/lib/node_exporter/scripts/salt-key-check.sh:
  file.managed:
    - name: /var/lib/node_exporter/scripts/salt-key-check.sh
    - source: salt://monitoring/textfiles/salt-key-check.sh
    - user: "root"
    - group: "root"
    - mode: 700
    - onlyif: test -d /srv/salt

salt_key_check:
  cron.present:
    - name: /var/lib/node_exporter/scripts/salt-key-check.sh
    - user: root
    - minute: '*/20'
    - require:
        - file: /var/lib/node_exporter/scripts/salt-key-check.sh

{%- endif %}