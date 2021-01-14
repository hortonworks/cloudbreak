{%- from 'telemetry/settings.sls' import telemetry with context %}
{% if telemetry.cdpTelemetryVersion > 2 %}
/opt/cdp-telemetry/conf/nodestatus-monitor.yaml:
   file.managed:
    - source: salt://nodestatus/template/nodestatus-monitor.yaml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600

start_nodestatus_monitor:
  service.running:
    - enable: True
    - name: cdp-nodestatus-monitor
    - watch:
       - file: /opt/cdp-telemetry/conf/nodestatus-monitor.yaml
{% endif %}