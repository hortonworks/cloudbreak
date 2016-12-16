include:
  - grafana.repo

grafana:
  pkg:
    - installed
    - pkgs: [grafana]
  service:
    - running
    - name: grafana-server
    - enable: True