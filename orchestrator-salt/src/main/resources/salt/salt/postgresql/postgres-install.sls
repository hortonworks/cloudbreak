start-postgresql:
  service.running:
    - enable: True
    - name: postgresql

/opt/salt/scripts/init_hive_db.sh:
  file.managed:
    - makedirs: True
    - mode: 755
    - source: salt://postgresql/scripts/init_hive_db.sh

init-hive-db:
  cmd.run:
    - name: /opt/salt/scripts/init_hive_db.sh
    - runas: postgres
    - env:
      - USER: {{ pillar['postgres']['user'] }}
      - PASSWORD: {{ pillar['postgres']['password'] }}
      - DATABASE:  {{ pillar['postgres']['database'] }}

reload-postgresql:
  cmd.run:
    - name: service postgresql reload
