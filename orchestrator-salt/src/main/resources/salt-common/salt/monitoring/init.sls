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

