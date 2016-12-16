/etc/yum.repos.d/grafana.repo:
  file.managed:
    - source: salt://grafana/yum/grafana.repo
    - template: jinja
