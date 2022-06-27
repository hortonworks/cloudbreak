{%- from 'telemetry/settings.sls' import telemetry with context %}
{%- from 'monitoring/settings.sls' import monitoring with context %}
{%- set cdp_prometheus_installed = salt['file.directory_exists' ]('/opt/cdp-prometheus') %}
include:
  - monitoring.scripts
{%- if monitoring.enabled and monitoring.remoteWriteUrl %}
  {%- if monitoring.useDevStack %}
  - monitoring.dev-stack
  {%- endif %}
  - monitoring.exporters
  - monitoring.textfiles
  - monitoring.request-signer
  {%- if cdp_prometheus_installed %}
  - monitoring.cdp-prometheus
  {%- else %}
  - monitoring.vmagent
  {%- endif %}

/etc/cron.d/monitoring_cert_check:
  file.managed:
    - user: root
    - group: root
    - mode: 600
    - source: salt://monitoring/cron/monitoring_cert_check

{%- endif %}
