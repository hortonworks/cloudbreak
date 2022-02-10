{%- from 'telemetry/settings.sls' import telemetry with context %}
{% if telemetry.databusEndpointValidation and telemetry.databusEndpoint and telemetry.cdpTelemetryVersion > 1 %}
check_databus_network_connectivity:
  cmd.run:
    - name: "cdp-telemetry utils check-connection --url {{ telemetry.databusEndpoint }}"
    - failhard: True{% if telemetry.proxyUrl %}
    - env:
       - HTTPS_PROXY: {{ telemetry.proxyUrl }}{% if telemetry.noProxyHosts and telemetry.cdpTelemetryVersion > 8 %}
       - NO_PROXY: {{ telemetry.noProxyHosts }}{% endif %}{% endif %}{% endif %}{% if telemetry.noProxyHosts and telemetry.cdpTelemetryVersion > 8 %}
/etc/cdp-telemetry/conf:
  file.directory:
    - makedirs: True

/etc/cdp-telemetry/conf/proxy-whitelist.txt:
    file.managed:
        - source: salt://telemetry/template/proxy-whitelist.txt.j2
        - template: jinja
        - mode: '0640'{% endif %}