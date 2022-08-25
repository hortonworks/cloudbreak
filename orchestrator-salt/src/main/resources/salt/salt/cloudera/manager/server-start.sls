{% if "manager_server" in grains.get('roles', []) %}

start-cloudera-scm-server:
  service.running:
    - name: cloudera-scm-server

{% endif %}
