/var/lib/node_exporter/scripts/cipa.py:
  file.managed:
    - source: salt://monitoring/scripts/cipa.py
    - user: "root"
    - group: "root"
    - mode: 700

cipa_metrics_cron:
  cron.present:
    - name: /var/lib/node_exporter/scripts/cipa.py
    - user: root
    - minute: '*/10'
    - require:
        - file: /var/lib/node_exporter/scripts/cipa.py