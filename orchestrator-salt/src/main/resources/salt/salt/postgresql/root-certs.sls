{%- from 'postgresql/settings.sls' import postgresql with context %}

{% if postgresql.root_certs_enabled == True %}
create-root-certs-file:
  file.managed:
    - name: {{ postgresql.root_certs_file }}
    - makedirs: True
    - contents_pillar: postgres_root_certs:ssl_certs
    - user: root
    - group: root
    - mode: 644

{% if postgresql.ssl_restart_required == True and postgresql.ssl_enabled == True and "manager_server" in grains.get('roles', []) %}
cm-server-restart-root-cert-changed:
  service.running:
    - name: cloudera-scm-server
    - watch:
        - file: create-root-certs-file
{% endif %}
{% endif %}