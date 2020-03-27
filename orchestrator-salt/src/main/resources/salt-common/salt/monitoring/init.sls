{%- from 'monitoring/settings.sls' import monitoring with context %}

{% if monitoring.enabled %}

/opt/metrics-collector:
  file.directory:
    - name: /opt/metrics-collector
    - user: "root"
    - group: "root"
    - mode: 755
    - recurse:
      - user
      - group
      - mode

/etc/metrics-collector:
  file.directory:
    - name: /etc/metrics-collector
    - user: "root"
    - group: "root"
    - mode: 755

/etc/metrics-collector/conf:
  file.directory:
    - name: /etc/metrics-collector/conf
    - user: "root"
    - group: "root"
    - mode: 740

/var/log/metrics-collector:
  file.directory:
    - name: /var/log/metrics-collector
    - user: "root"
    - group: "root"
    - mode: 740

install_pyyaml:
  cmd.run:
    - name: pip install PyYAML --ignore-installed
    - unless: pip list --no-index | grep -E 'PyYAML'

{%- if monitoring.type == "cloudera_manager" %}
install_cm_client:
  cmd.run:
    - name: pip install cm-client==40.0.3 --ignore-installed
    - unless: pip list --no-index | grep -E 'cm-client.*40.0.3'

/opt/metrics-collector/cm_metrics_collector.py:
   file.managed:
    - source: salt://monitoring/scripts/cm_metrics_collector.py
    - user: "root"
    - group: "root"
    - mode: 740
{% endif %}

/opt/metrics-collector/metrics_collector.py:
   file.managed:
    - source: salt://monitoring/scripts/metrics_collector.py
    - user: "root"
    - group: "root"
    - mode: 740

/opt/metrics-collector/metrics_logger.py:
   file.managed:
    - source: salt://monitoring/scripts/metrics_logger.py
    - user: "root"
    - group: "root"
    - mode: 740

/opt/metrics-collector/retry.py:
   file.managed:
    - source: salt://monitoring/scripts/retry.py
    - user: "root"
    - group: "root"
    - mode: 740

{%- if monitoring.type == "cloudera_manager" %}
/etc/metrics-collector/conf/metrics-collector.yaml:
   file.managed:
    - source: salt://monitoring/template/metrics-collector.yaml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600

{% endif %}

{%- if monitoring.is_systemd %}
/etc/systemd/system/metrics-collector.service:
   file.managed:
    - source: salt://monitoring/template/metrics-collector.service.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600

{%- if monitoring.type == "cloudera_manager" %}
/etc/systemd/system/metrics-collector.d:
  file.directory:
    - name: /etc/systemd/system/metrics-collector.d
    - user: "root"
    - group: "root"
    - mode: 740
{% endif %}
start_monitoring:
  service.running:
    - enable: True
    - name: metrics-collector
    - watch:
       - file: /etc/systemd/system/metrics-collector.service
{% endif %}

{% endif %}

