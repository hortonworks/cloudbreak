include:
  - grafana.repo

grafana_install:
  pkg:
    - installed
    - pkgs:
      - grafana

/etc/init.d/grafana-server:
  file.managed:
    - source: salt://grafana/initd/grafana-server
    - mode: 744

grafana_start:
  service:
    - running
    - name: grafana-server
    - enable: True