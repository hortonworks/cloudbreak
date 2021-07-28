{%- from 'nodestatus/settings.sls' import nodestatus with context %}
{% if nodestatus.collectAvailable %}
/opt/cdp-telemetry/conf/nodestatus-monitor.yaml:
   file.managed:
    - source: salt://nodestatus/template/nodestatus-monitor.yaml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600{% if nodestatus.doctorTimeoutSupported %}
start_nodestatus_monitor:
  service.running:
    - enable: True
    - name: cdp-nodestatus-monitor
    - watch:
       - file: /opt/cdp-telemetry/conf/nodestatus-monitor.yaml{% else %}
stop_nodestatus_monitor:
  service.dead:
    - enable: False
    - name: cdp-nodestatus-monitor{% endif %}
{% endif %}