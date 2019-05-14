{%- from 'fluent/settings.sls' import fluent with context %}
{% if fluent.enabled %}
fluent_stop:
  service.dead:
    - enable: False
    - name: td-agent
{% endif %}