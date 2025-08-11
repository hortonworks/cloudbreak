{%- from 'telemetry/settings.sls' import telemetry with context %}
{%- from 'monitoring/settings.sls' import monitoring with context %}

{%- if monitoring.enabled and monitoring.nodeExporterExists %}

/var/lib/node_exporter/files:
  file.directory:
    - name: /var/lib/node_exporter/files
    - user: "root"
    - group: "root"
    - mode: 750
    - failhard: True
    - makedirs: True

/var/lib/node_exporter/scripts:
  file.directory:
    - name: /var/lib/node_exporter/scripts
    - user: "root"
    - group: "root"
    - mode: 750
    - failhard: True
    - makedirs: True

/etc/systemd/system/cdp-node-exporter.service:
  file.managed:
    - source: salt://monitoring/systemd/cdp-node-exporter.service.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 640

generate_node_exporter_cert_and_key:
  cmd.run:
    - name: "/opt/salt/scripts/cert-helper.sh -b /opt/node_exporter/node_exporter"

{%- if monitoring.localPassword %}
/opt/node_exporter/node_pwd:
  file.managed:
    - source: salt://monitoring/template/exporter_pwd_file.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600

{%- else %}
remove_node_exporter_auth:
  file.absent:
    - name: /opt/node_exporter/basic_auth_cred
remove_node_exporter_pwd:
  file.absent:
    - name: /opt/node_exporter/node_pwd
{%- endif %}

/opt/node_exporter/node_exporter-web-config.yml:
  file.managed:
    - source: salt://monitoring/template/node_exporter-web-config.yml.j2
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
      - file: /opt/node_exporter/node_exporter-web-config.yml

{%- else %}
kill_node_exporter:
  service.dead:
    - name: "cdp-node-exporter"
{%- endif %}

{%- if monitoring.enabled and monitoring.blackboxExporterExists %}
/etc/systemd/system/cdp-blackbox-exporter.service:
  file.managed:
    - source: salt://monitoring/systemd/cdp-blackbox-exporter.service.j2
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

generate_blackbox_exporter_cert_and_key:
  cmd.run:
    - name: "/opt/salt/scripts/cert-helper.sh -b /opt/blackbox_exporter/blackbox_exporter"

{%- if monitoring.localPassword %}
/opt/blackbox_exporter/blackbox_pwd:
  file.managed:
    - source: salt://monitoring/template/exporter_pwd_file.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600
{%- else %}
remove_blackbox_exporter_auth:
  file.absent:
    - name: /opt/blackbox_exporter/basic_auth_cred
remove_blackbox_exporter_pwd:
  file.absent:
    - name: /opt/blackbox_exporter/blackbox_pwd
{%- endif %}

/opt/blackbox_exporter/blackbox_exporter-web-config.yml:
  file.managed:
    - source: salt://monitoring/template/blackbox_exporter-web-config.yml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600

start_blackbox_exporter:
  service.running:
    - enable: True
    - name: "cdp-blackbox-exporter"
    - watch:
      - file: /opt/blackbox_exporter/blackbox.yml
      - file: /opt/blackbox_exporter/blackbox.env
      - file: /opt/blackbox_exporter/blackbox_exporter-web-config.yml
      - file: /etc/systemd/system/cdp-blackbox-exporter.service

{%- else %}
kill_blackbox_exporter:
  service.dead:
    - name: "cdp-blackbox-exporter"
{%- endif %}