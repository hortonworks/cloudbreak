{%- from 'telemetry/settings.sls' import telemetry with context %}
{%- from 'monitoring/settings.sls' import monitoring with context %}

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

{%- if monitoring.enabled and monitoring.remoteWriteUrl %}
include:
  {%- if monitoring.useDevStack %}
  - monitoring.dev-stack
  {%- endif %}
  - monitoring.exporters
  - monitoring.vmagent
{%- endif %}
