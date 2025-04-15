{%- set os = salt['grains.get']('os') %}
{%- set osMajorRelease = salt['grains.get']('osmajorrelease') | int %}
{%- set platform = salt['pillar.get']('platform') %}
{%- set lbEnabled = salt['pillar.get']('freeipa:loadBalancer:enabled', False) %}
{%- if lbEnabled and platform == 'GCP' and os == "RedHat" and osMajorRelease == 8 %}
/opt/salt/scripts/loadbalancer_ip.sh:
  file.managed:
    - source: salt://loadbalancer/scripts/loadbalancer_ip.sh.j2
    - template: jinja
    - makedirs: True
    - user: root
    - group: root
    - mode: 700

create_loopback_service_unit:
  file.managed:
    - name: /etc/systemd/system/loopback.service
    - source: salt://loadbalancer/services/loopback.service.j2
    - template: jinja
    - makedirs: True
    - user: root
    - group: root
    - mode: 664
    - require:
      - file: /opt/salt/scripts/loadbalancer_ip.sh

start_and_enable_loopback_service:
  service.running:
    - name: loopback
    - enable: True
    - require:
      - file: /opt/salt/scripts/loadbalancer_ip.sh
      - file: /etc/systemd/system/loopback.service
{%- endif %}
