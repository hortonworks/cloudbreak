{%- from 'telemetry/settings.sls' import telemetry with context %}
{%- from 'monitoring/settings.sls' import monitoring with context %}

{%- if monitoring.enabled %}

/etc/systemd/system/cdp-node-exporter.service:
  file.managed:
    - source: salt://monitoring/systemd/cdp-node-exporter.service
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 640

/etc/systemd/system/cdp-blackbox-exporter.service:
  file.managed:
    - source: salt://monitoring/systemd/cdp-blackbox-exporter.service
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 640

/opt/blackbox_exporter/blackbox.yml:
  file.managed:
    - source: salt://monitoring/template/blackbox.yml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 640

/opt/blackbox_exporter/blackbox.env:
  file.managed:
    - source: salt://monitoring/template/blackbox.env.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600

start_node_exporter:
  service.running:
    - enable: True
    - name: "cdp-node-exporter"
    - watch:
      - file: /etc/systemd/system/cdp-node-exporter.service

start_blackbox_exporter:
  service.running:
    - enable: True
    - name: "cdp-blackbox-exporter"
    - watch:
      - file: /opt/blackbox_exporter/blackbox.yml
      - file: /opt/blackbox_exporter/blackbox.env
      - file: /etc/systemd/system/cdp-blackbox-exporter.service

{%- endif %}