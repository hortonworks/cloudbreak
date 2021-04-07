{%- from 'nodestatus/settings.sls' import nodestatus with context %}
{% if nodestatus.collectAvailable %}
nodestatus_collect_telemetry:
    cmd.run:
        - name: cdp-nodestatus collect {{ nodestatus.collectParams }}{% endif %}