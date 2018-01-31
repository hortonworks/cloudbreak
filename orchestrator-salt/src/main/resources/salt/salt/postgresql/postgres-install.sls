install-postgres:
  pkg.installed:
    - pkgs:
      - postgresql-server
      - postgresql-jdbc
      - postgresql

start-postgresql:
  service.running:
    - enable: True
    - name: postgresql

init-hive-db:
  cmd.script:
    - source: salt://postgresql/scripts/init_hive_db.sh
    - runas: postgres
    - env:
      - USER: {{ pillar['postgres']['user'] }}
      - PASSWORD: {{ pillar['postgres']['password'] }}
      - DATABASE:  {{ pillar['postgres']['database'] }}
