/opt/scripts/slider/SLIDER-942.1.diff:
  file.managed:
    - makedirs: True
    - source: salt://slider/scripts/SLIDER-942.1.diff
    - mode: 755

/opt/scripts/slider/slider-patch.sh:
  file.managed:
    - makedirs: True
    - source: salt://slider/scripts/slider-patch.sh
    - mode: 755
    - watch:
      - file: /opt/scripts/slider/SLIDER-942.1.diff

execute-patch:
  cmd.run:
    - name: /opt/scripts/slider/slider-patch.sh
    - watch:
      - file: /opt/scripts/slider/slider-patch.sh