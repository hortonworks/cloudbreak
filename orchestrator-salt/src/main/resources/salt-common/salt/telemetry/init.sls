{%- from 'telemetry/settings.sls' import telemetry with context %}
{% if telemetry.databusEndpointValidation and telemetry.databusEndpoint and telemetry.cdpTelemetryVersion > 1 %}
check_databus_network_connectivity:
  cmd.run:
    - name: "cdp-telemetry utils check-connection --url {{ telemetry.databusEndpoint }}"
    - failhard: True{% if telemetry.proxyUrl %}
    - env: {% if telemetry.proxyProtocol == "https" %}
       - HTTPS_PROXY: {{ telemetry.proxyUrl }}{% else %}
       - HTTP_PROXY: {{ telemetry.proxyUrl }}{% endif %}{% endif %}
{% endif %}