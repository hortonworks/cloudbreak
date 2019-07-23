{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}

start_agent:
  service.running:
    - enable: True
    - name: cloudera-scm-agent
{% if cloudera_manager.communication.autotls_enabled == True %}
    - onlyif: test -f /etc/cloudera-scm-agent/cmagent.token
{% endif %}
