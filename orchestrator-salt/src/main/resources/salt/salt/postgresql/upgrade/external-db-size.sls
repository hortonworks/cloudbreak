{%- from 'postgresql/settings.sls' import postgresql with context %}

set_pgpassword:
  environ.setenv:
    - name: PGPASSWORD
    - value: {{ pillar['postgres']['clouderamanager']['remote_admin_pw'] }}
    - update_minion: True

{% if postgresql.ssl_enabled == True %}
set_pgsslrootcert:
  environ.setenv:
    - name: PGSSLROOTCERT
    - value: {{ postgresql.root_certs_file }}
    - update_minion: True
set_pgsslmode:
  environ.setenv:
    - name: PGSSLMODE
    - value: {{ postgresql.ssl_verification_mode }}
    - update_minion: True
{%- endif %}

get_external_db_size:
  cmd.run:
    - name: psql --host="{{salt['pillar.get']('postgres:clouderamanager:remote_db_url')}}" --port="{{salt['pillar.get']('postgres:clouderamanager:remote_db_port')}}" --username="{{salt['pillar.get']('postgres:clouderamanager:remote_admin')}}" --dbname="postgres" -qtA -c "SELECT sum(pg_database_size(pg_database.datname)) FROM pg_database WHERE pg_database.datacl is null or  CAST(pg_database.datacl as TEXT) NOT LIKE '%azure_superuser%';"
    - require:
      - environ: set_pgpassword
{% if postgresql.ssl_enabled == True %}
      - environ: set_pgsslrootcert
      - environ: set_pgsslmode
{%- endif %}