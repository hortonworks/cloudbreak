{%- from 'monitoring/settings.sls' import monitoring with context %}
{%- if monitoring.enabled and monitoring.remoteWriteUrl %}
monitoring_agent_stop:
  service.dead:
    - enable: False
    - name: cdp-prometheus

monitoring_request_signer_stop:
  service.dead:
    - enable: False
    - name: cdp-request-signer

monitoring_nodeexporter_stop:
  service.dead:
    - enable: False
    - name: cdp-node-exporter

monitoring_blackboxexporter_stop:
  service.dead:
    - enable: False
    - name: cdp-blackbox-exporter
{%- endif %}
