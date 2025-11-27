{%- from 'minifi/settings.sls' import minifi with context %}

{% if minifi.enabled %}
include:
  - minifi.minifi-init
{% else %}
warning_minifi_disabled:
   cmd.run:
    - name: echo "Warning - Minifi based logging is not enabled."
{% endif %}
