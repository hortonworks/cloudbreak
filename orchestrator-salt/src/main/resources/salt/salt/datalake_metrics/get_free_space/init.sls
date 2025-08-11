/opt/salt/scripts/get_free_space.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - source: salt://datalake_metrics/get_free_space/scripts/get_free_space.sh
