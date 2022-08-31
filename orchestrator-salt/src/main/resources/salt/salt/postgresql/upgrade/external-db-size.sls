include:
  - postgresql.upgrade

get_external_db_size:
  cmd.run:
    - name: psql --host="{{salt['pillar.get']('postgres:clouderamanager:remote_db_url')}}" --port="{{salt['pillar.get']('postgres:clouderamanager:remote_db_port')}}" --username="{{salt['pillar.get']('postgres:clouderamanager:remote_admin')}}" --dbname="postgres" -qtA -c "SELECT sum(pg_database_size(pg_database.datname)) FROM pg_database WHERE pg_database.datacl is null or  CAST(pg_database.datacl as TEXT) NOT LIKE '%azure_superuser%';"
    - require:
        - sls: postgresql.upgrade