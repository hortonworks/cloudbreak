{%- from 'metering/settings.sls' import metering with context %}
{%- from 'nodes/settings.sls' import host with context %}
{% set os = salt['grains.get']('os') %}
{% set metering_package_name = 'metering-heartbeat-application' %}
{% set metering_package_version = '0.1-SNAPSHOT_652d2a8abc3132092b718c5f0cd24ef235f10910' %}
{% set metering_rmp_repo_url = 'https://cloudera-service-delivery-cache.s3-us-west-2.amazonaws.com/metering/heartbeat_producer/'%}
{% set metering_rpm_location = metering_rmp_repo_url + metering_package_name + '-' + metering_package_version + '.x86_64.rpm' %}

{% if metering.enabled %}

{%- if os == "RedHat" or os == "CentOS" %}

{% if metering.is_systemd %}

install_metering_rpm_manually:
  cmd.run:
    - name: "rpm -i {{ metering_rpm_location }}"
    - onlyif: "! rpm -q {{ metering_package_name }}"

{% if salt['file.file_exists' ]('/lib/systemd/system/metering-heartbeat-application.service') %}
stop_metering_heartbeat_application:
  service.dead:
    - enable: False
    - name: "{{ metering_package_name }}"
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
    - name: "{{ metering_package_name }}"
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