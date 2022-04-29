{%- from 'telemetry/settings.sls' import telemetry with context %}
{%- from 'monitoring/settings.sls' import monitoring with context %}
include:
  - monitoring.scripts
{%- if monitoring.enabled and monitoring.remoteWriteUrl %}
  {%- if monitoring.useDevStack %}
  - monitoring.dev-stack
  {%- endif %}
  - monitoring.exporters
  - monitoring.vmagent

/etc/cron.d/monitoring_cert_check:
  file.managed:
    - user: root
    - group: root
    - mode: 600
    - source: salt://monitoring/cron/monitoring_cert_check

{%- endif %}
