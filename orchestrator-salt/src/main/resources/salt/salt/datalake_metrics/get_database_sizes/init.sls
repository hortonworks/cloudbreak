/opt/salt/scripts/get_database_sizes.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - source: salt://datalake_metrics/get_database_sizes/scripts/get_database_sizes.sh

/opt/salt/postgresql/.pgpass:
  file.managed:
    - makedirs: True
    - mode: 600
    - source: salt://postgresql/disaster_recovery/.pgpass
    - template: jinja

set_pgpass_file:
  environ.setenv:
    - name: PGPASSFILE
    - value: /opt/salt/postgresql/.pgpass
    - require:
      - file: /opt/salt/postgresql/.pgpass
