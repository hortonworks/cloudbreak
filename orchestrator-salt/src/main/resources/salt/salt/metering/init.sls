{%- from 'metering/settings.sls' import metering with context %}
{%- from 'nodes/settings.sls' import host with context %}
{% set os = salt['grains.get']('os') %}
{% set metering_rmp_repo_url = 'https://cloudera-service-delivery-cache.s3-us-west-2.amazonaws.com/metering/heartbeat_producer/'%}
{% set metering_rpm_location = metering_rmp_repo_url + 'metering-heartbeat-application-0.1-SNAPSHOT_191281a19a4ca403a93294514da847cdb160549d.x86_64.rpm' %}

{% if metering.enabled %}

{%- if os == "RedHat" or os == "CentOS" %}

{% if metering.is_systemd %}

{% if not salt['file.directory_exists' ]('/etc/metering') %}
install_metering_rpm_manually:
  cmd.run:
    - name: "rpm -i {{ metering_rpm_location }}"
{% endif %}

{% if salt['file.file_exists' ]('/lib/systemd/system/metering-heartbeat-application.service') %}
stop_metering_heartbeat_application:
  service.dead:
    - enable: False
    - name: metering-heartbeat-application
    - onlyif: "test -f /etc/metering/generate_heartbeats.ini && ! grep -q 'CONFIGURED BY SALT' /etc/metering/generate_heartbeats.ini"

/etc/metering:
  file.directory:
    - name: /etc/metering
    - user: "root"
    - group: "root"
    - mode: 740
    - recurse:
      - user
      - group
      - mode

/var/log/metering:
  file.directory:
    - name: /var/log/metering
    - user: "root"
    - group: "root"
    - mode: 740
    - recurse:
      - user
      - group
      - mode

/etc/metering/generate_heartbeats.ini:
  file.managed:
    - source: salt://metering/template/generate_heartbeats.ini.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - file_mode: 640

start_metering_heartbeat_application:
  service.running:
    - enable: True
    - name: metering-heartbeat-application
{% endif %}

{% else %}
warning_metering_systemd:
  cmd.run:
    - name: "Warning - Metering won't be installed/used as it requires systemd"
{% endif %}
{% else %}
warning_metering_os:
  cmd.run:
    - name: "Warning - Metering is not supported on this OS ({{ os }})"
{% endif %}

{% endif %}