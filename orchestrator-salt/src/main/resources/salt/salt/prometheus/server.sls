include:
    - prometheus.user

download_prom_server:
  archive.extracted:
    - name: /usr/local/bin/
    - source: {{ salt['pillar.get']('prometheus:server:source') }}
    - source_hash: {{ salt['pillar.get']('prometheus:server:source_hash')}}
    - keep: True
    - archive_format: tar
    - options: z --strip-components=1

/opt/prometheus:
    file.directory:
      - user: prometheus
      - group: prometheus

/var/log/prometheus/:
  file.directory:
    - user: prometheus
    - group: prometheus
    - makedirs: True

/etc/prometheus/:
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

{% if salt['grains.get']('init') == 'systemd' %}
prometheus_server_service_script:
  file.managed:
    - name: /etc/systemd/system/prometheus.service
    - user: root
    - group: root
    - contents: |
        [Unit]
        Description=prometheus
        After=syslog.target network.target

        [Service]
        Type=simple
        RemainAfterExit=no
        WorkingDirectory=/opt/prometheus
        User=prometheus
        Group=prometheus
        ExecStart=/usr/local/bin/prometheus -config.file=/etc/prometheus/prometheus.yaml  -storage.local.path=/opt/prometheus/data -log.level info

        [Install]
        WantedBy=multi-user.target
{% else %}
/etc/init.d/prometheus:
  file.managed:
    - makedirs: True
    - source: salt://prometheus/init.d/prometheus
    - template: jinja
    - mode: 755

{% endif %}

prometheus_server_service:
  service.running:
    - name: prometheus
    - enable: True