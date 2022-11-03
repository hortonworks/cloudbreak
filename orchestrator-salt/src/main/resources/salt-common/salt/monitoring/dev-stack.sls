{%- from 'monitoring/settings.sls' import monitoring with context %}
{%- if salt['grains.get']('master')[0] == salt['grains.get']('fqdn_ip4')[0] %}
/opt/dev-prometheus:
  file.directory:
    - name: /opt/dev-prometheus
    - mode: 740

/opt/dev-prometheus/data:
  file.directory:
    - name: /opt/dev-prometheus/data
    - mode: 740

install_prometheus:
  archive.extracted:
    - name: /opt/dev-prometheus/
    - source: https://github.com/prometheus/prometheus/releases/download/v2.34.0/prometheus-2.34.0.linux-amd64.tar.gz
    - archive_format: tar
    - enforce_toplevel: False
    - source_hash: md5=4d065c3b71824122272748d898aae779
    - options: --strip-components=1
    - if_missing: /opt/dev-prometheus/prometheus

/opt/dev-prometheus/prometheus.yml:
  file.managed:
    - user: root
    - group: root
    - mode: 600
    - source: salt://monitoring/dev/prometheus.yml

/etc/systemd/system/dev-prometheus.service:
  file.managed:
    - source: salt://monitoring/systemd/dev-prometheus.service
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 640

start_dev_prometheus:
  service.running:
    - enable: True
    - name: dev-prometheus
    - watch:
      - file: /etc/systemd/system/dev-prometheus.service

/etc/yum.repos.d/dev-grafana.repo:
  file.managed:
    - source: salt://monitoring/dev/dev-grafana.repo
    - template: jinja
    - mode: 640

clean_grafana_repo:
  cmd.run:
    - name: yum clean all --disablerepo="*" --enablerepo=grafana

install_grafana:
  pkg.installed:
    - pkgs:
        - grafana

start_dev_grafana:
  service.running:
    - enable: True
    - name: grafana-server
{%- endif %}
