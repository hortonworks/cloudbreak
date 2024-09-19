{%- from 'fluent/settings.sls' import fluent with context %}

{% if fluent.enabled %}
include:
  - fluent.cdp-logging-agent-init
{% else %}
warning_fluentd_disabled:
   cmd.run:
    - name: echo "Warning - FluentD based logging is not enabled."
{% endif %}
