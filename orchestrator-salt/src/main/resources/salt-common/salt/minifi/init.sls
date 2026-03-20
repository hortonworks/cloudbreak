{%- from 'minifi/settings.sls' import minifi with context %}

{% if minifi.enabled and minifi.preferMinifiLogging %}
include:
  - minifi.minifi-init
{% else %}
ensure_minifi_is_disabled_as_requested:
  service.dead:
    - name: minifi
    - enable: False
    - onlyif: systemctl list-unit-files | grep -q "^minifi.service"

warning_minifi_disabled:
   cmd.run:
    - name: echo "Warning - Minifi based logging is not enabled."
{% endif %}
