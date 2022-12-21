{%- from 'postgresql/settings.sls' import postgresql with context %}

{% if postgresql.ssl_enabled == True %}
create-root-certs-file:
  file.managed:
    - name: {{ postgresql.root_certs_file }}
    - makedirs: True
    - contents_pillar: postgres_root_certs:ssl_certs
    - user: root
    - group: root
    - mode: 644
    - unless: test -f {{ postgresql.root_certs_file }}
{% endif %}