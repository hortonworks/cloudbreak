{%- from 'telemetry/settings.sls' import telemetry with context %}
{% set nodestatus = {} %}
{% set server_username = salt['pillar.get']('nodestatus:serverUsername') %}
{% set server_password = salt['pillar.get']('nodestatus:serverPassword') %}

{% if telemetry.databusEndpoint %}
    {% set databus_params = "--databus-url " + telemetry.databusEndpoint %}
{% else %}
    {% set databus_params = "" %}
{% endif %}
{% if telemetry.cdpTelemetryVersion > 6 and (telemetry.platform == "AWS" or telemetry.platform == "AZURE" or telemetry.platform == "GCP") %}
    {% set additional_collect_params = "--cloud-provider " + telemetry.platform %}
{% else %}
    {% set additional_collect_params = "" %}
{% endif %}

{% set collect_params = databus_params + " " + additional_collect_params %}

{% if telemetry.cdpTelemetryVersion > 2 %}
  {% set collect_available = True %}
{% else %}
  {% set collect_available = False %}
{% endif %}

{% if telemetry.cdpTelemetryVersion > 4 %}
  {% set salt_ping_available = True %}
{% else %}
  {% set salt_ping_available = False %}
{% endif %}

{% do nodestatus.update({
    "serverUsername": server_username,
    "serverPassword": server_password,
    "collectParams": collect_params,
    "collectAvailable": collect_available,
    "saltPingAvailable": salt_ping_available
}) %}