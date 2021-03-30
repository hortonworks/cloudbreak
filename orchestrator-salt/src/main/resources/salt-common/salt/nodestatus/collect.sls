{%- from 'telemetry/settings.sls' import telemetry with context %}
{% if telemetry.cdpTelemetryVersion > 2 %}
nodestatus_collect_telemetry:
    cmd.run:{% if telemetry.databusEndpoint %}
        - name: cdp-nodestatus collect --databus-url {{ telemetry.databusEndpoint }}{% else %}
        - name: cdp-nodestatus collect{% endif %}{% endif %}