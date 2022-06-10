/opt/salt/scripts/check_atlas_updated.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - source: salt://atlas/scripts/check_atlas_updated.sh
