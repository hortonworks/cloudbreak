{%- from 'telemetry/settings.sls' import telemetry with context %}
{%- from 'monitoring/settings.sls' import monitoring with context %}
{%- set cdp_prometheus_installed = salt['file.directory_exists' ]('/opt/cdp-prometheus') %}

{%- if monitoring.enabled and cdp_prometheus_installed %}
generate_cdp_prometheus_cert_and_key:
  cmd.run:
    - name: "/opt/salt/scripts/cert-helper.sh -b /opt/cdp-prometheus/cdp-prometheus"

{%- if monitoring.localPassword %}
/opt/cdp-prometheus/prometheus_pwd:
  file.managed:
    - source: salt://monitoring/template/exporter_pwd_file.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600
{%- else %}
remove_prometheus_auth:
  file.absent:
    - name: /opt/cdp-prometheus/basic_auth_cred
remove_prometheus_pwd:
  file.absent:
    - name: /opt/cdp-prometheus/prometheus_pwd
{%- endif %}

/etc/systemd/system/cdp-prometheus.service:
  file.managed:
    - source: salt://monitoring/systemd/cdp-prometheus.service.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 640

/opt/cdp-prometheus/prometheus.yml:
  file.managed:
    - source: salt://monitoring/template/prometheus.yml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 640

/opt/cdp-prometheus/prometheus-web-config.yml:
  file.managed:
    - source: salt://monitoring/template/prometheus-web-config.yml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 640

{%- if monitoring.requestSignerEnabled %}
/opt/cdp-prometheus/request_signer_pwd_file:
  file.managed:
    - source: salt://monitoring/template/request_signer_pwd_file.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600
{%- else %}
  {%- if monitoring.password %}
/opt/cdp-prometheus/remote_pwd_file:
  file.managed:
    - source: salt://monitoring/template/remote_pwd_file.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600
  {%- elif monitoring.token %}
/opt/cdp-prometheus/remote_token_file:
  file.managed:
    - source: salt://monitoring/template/remote_token_file.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600
  {%- endif %}
{%- endif %}

/var/log/cdp-prometheus:
  file.directory:
    - user: "root"
    - group: "root"
    - makedirs: True
    - mode: 755

start_cdp_prometheus:
  service.running:
    - enable: True
    - name: "cdp-prometheus"
    - watch:
      - file: /etc/systemd/system/cdp-prometheus.service
      - file: /opt/cdp-prometheus/prometheus.yml
      - file: /opt/cdp-prometheus/prometheus-web-config.yml
{%- if monitoring.requestSignerEnabled %}
      - file: /opt/cdp-prometheus/request_signer_pwd_file
{%- else %}
  {%- if monitoring.password %}
      - file: /opt/cdp-prometheus/remote_pwd_file
  {%- elif monitoring.token %}
      - file: /opt/cdp-prometheus/remote_token_file
  {%- endif %}
{%- endif %}
{%- else %}
kill_cdp_prometheus:
  service.dead:
    - name: "cdp-prometheus"

delete_config_files:
  file.absent:
    - names:
      - /opt/cdp-prometheus/prometheus.yml
      - /opt/cdp-prometheus/prometheus-web-config.yml
      - /opt/cdp-prometheus/request_signer_pwd_file
      - /opt/cdp-prometheus/remote_pwd_file
      - /opt/cdp-prometheus/remote_token_file
      - /etc/systemd/system/cdp-prometheus.service
{%- endif %}