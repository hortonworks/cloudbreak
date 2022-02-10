{%- from 'telemetry/settings.sls' import telemetry with context %}
{%- from 'monitoring/settings.sls' import monitoring with context %}

{% if monitoring.enabled and telemetry.cdpTelemetryVersion > 6 %}

{%- if monitoring.type == "cloudera_manager" %}
/opt/cdp-telemetry/conf/metrics-collector.yaml:
   file.managed:
    - source: salt://monitoring/template/metrics-collector.yaml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600

start_monitoring:
  service.running:
    - enable: True
    - name: cdp-metrics-collector
    - watch:
       - file: /opt/cdp-telemetry/conf/metrics-collector.yaml
{% endif %}

{% endif %}
/opt/salt/scripts/cdp_resources_check.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://monitoring/scripts/cdp_resources_check.sh

/etc/cron.d/cdp_resources_check:
  file.managed:
    - user: root
    - group: root
    - mode: 600
    - source: salt://monitoring/cron/cdp_resources_check

