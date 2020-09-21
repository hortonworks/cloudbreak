{%- from 'fluent/settings.sls' import fluent with context %}

{% if fluent.enabled %}
{% if fluent.binary == 'td-agent' %}
include:
  - fluent.td-agent-init
{% else %}
include:
  - fluent.cdp-logging-agent-init
{% endif %}
{% else %}
warning_fluentd_disabled:
   cmd.run:
    - name: echo "Warning - FluentD based logging is not enabled."
{% endif %}
