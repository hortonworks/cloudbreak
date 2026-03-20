{%- from 'fluent/settings.sls' import fluent with context %}

{% if fluent.enabled and not fluent.preferMinifiLogging %}
include:
  - fluent.cdp-logging-agent-init
{% else %}
ensure_cdp_logging_agent_is_disabled_as_requested:
  service.dead:
    - name: cdp-logging-agent
    - enable: False
    - onlyif: systemctl list-unit-files | grep -q "^cdp-logging-agent.service"

warning_fluentd_disabled:
   cmd.run:
    - name: echo "Warning - FluentD based logging is not enabled."
{% endif %}
