{%- from 'metadata/settings.sls' import metadata with context %}
{%- from 'postgresql/settings.sls' import postgresql with context %}

{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}
{% set postgres_directory = salt['pillar.get']('postgres:postgres_directory') %}
{% set postgres_data_on_attached_disk = salt['pillar.get']('postgres:postgres_data_on_attached_disk', 'False') %}

{% if 'None' == configure_remote_db %}
{%- if postgres_data_on_attached_disk %}
{%- if postgresql.ssl_enabled == True %}

/opt/salt/scripts/rotate_embeddeddb_certificate.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: postgres
    - mode: 750
    - source: salt://postgresql/embeddeddb-cert-rotate/scripts/rotate_embeddeddb_certificate.sh
    - template: jinja
    - context:
        postgres_directory: {{ postgres_directory }}

rotate_embeddeddb_certificate:
  cmd.run:
    - name: /opt/salt/scripts/rotate_embeddeddb_certificate.sh 2>&1 | tee -a /var/log/rotate_embeddeddb_certificate.log && exit ${PIPESTATUS[0]}
    - require:
      - file: /opt/salt/scripts/rotate_embeddeddb_certificate.sh

{%- endif %}
{%- endif %}
{% endif %}
