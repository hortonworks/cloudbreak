{%- from 'fluent/settings.sls' import fluent with context %}
{% if fluent.enabled %}
{% if fluent.preferMinifiLogging %}
minifi_stop:
  service.dead:
    - name: minifi
    - enable: False
    - onlyif: systemctl list-unit-files | grep -q "^minifi.service"
{% else %}
fluent_stop:
  service.dead:
    - enable: False
    {% if fluent.binary == 'td-agent'%}
    - name: td-agent
    {% else %}
    - name: cdp-logging-agent
    {% endif %}
{% endif %}
{% endif %}