{% if "manager_agent" in grains.get('roles', []) %}

stop_agent:
  service.dead:
    - enable: False
    - name: cloudera-scm-agent

{% endif %}