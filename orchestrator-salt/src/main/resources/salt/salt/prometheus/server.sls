include:
    - prometheus.user

/opt/prometheus:
    file.directory:
      - user: prometheus
      - group: prometheus

/var/log/prometheus/:
  file.directory:
    - user: prometheus
    - group: prometheus
    - makedirs: True

/etc/prometheus/rules:
  file.directory:
    - user: prometheus
    - group: prometheus
    - makedirs: True

prometheus_server_config:
  file.serialize:
    - name: /etc/prometheus/prometheus.yml
    - user: prometheus
    - group: prometheus
    - mode: 640
    - dataset_pillar: prometheus:server:config

prometheus_server_service:
  service.running:
    - name: prometheus
    - enable: True