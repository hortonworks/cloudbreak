{% if "manager_server" in grains.get('roles', []) %}

stop-cloudera-scm-server:
  service.dead:
    - name: cloudera-scm-server

{% endif %}
