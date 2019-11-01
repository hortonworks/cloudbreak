{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}

{% if cloudera_manager.communication.autotls_enabled == True %}
check_token:
  file.exists:
    - name: /etc/cloudera-scm-agent/cmagent.token
    - retry:
        attempts: 10
{% endif %}

start_agent:
  service.running:
    - enable: True
    - name: cloudera-scm-agent
{% if cloudera_manager.communication.autotls_enabled == True %}
    - require:
        - file: check_token
{% endif %}
