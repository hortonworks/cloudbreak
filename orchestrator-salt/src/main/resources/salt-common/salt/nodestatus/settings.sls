{%- from 'telemetry/settings.sls' import telemetry with context %}
{% set nodestatus = {} %}
{% set server_username = salt['pillar.get']('nodestatus:serverUsername') %}
{% set server_password = salt['pillar.get']('nodestatus:serverPassword') %}
{% set salt_ping_enabled = salt['pillar.get']('nodestatus:saltPingEnabled') %}

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

{% if telemetry.clusterType and telemetry.clusterType|upper != "DATAHUB" %}
  {% set doctor_timeout_supported = True %}
{% elif telemetry.cdpTelemetryPackageVersion and salt['pkg.version_cmp'](telemetry.cdpTelemetryPackageVersion,'0.4.10-1') >= 0 %}
  {% set doctor_timeout_supported = True %}
{% else %}
  {% set doctor_timeout_supported = False %}
{% endif %}

{% if telemetry.cdpTelemetryVersion > 9 %}
  {% set salt_ping_available = True %}
{% else %}
  {% set salt_ping_available = False %}
{% endif %}

{% do nodestatus.update({
    "serverUsername": server_username,
    "serverPassword": server_password,
    "collectParams": collect_params,
    "collectAvailable": collect_available,
    "doctorTimeoutSupported": doctor_timeout_supported,
    "saltPingAvailable": salt_ping_available,
    "saltPingEnabled": salt_ping_enabled
}) %}