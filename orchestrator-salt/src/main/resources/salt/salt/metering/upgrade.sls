{% set rpm_file_name = 'thunderhead-metering-heartbeat-application-0.1-SNAPSHOT.x86_64.rpm' %}
{% set date_from = salt['pillar.get']('metering:upgradeDateFrom', '2022-01-18') %}
{% set custom_rpm_url = salt['pillar.get']('metering:customRpmUrl', 'https://archive.cloudera.com/cp_clients/' + rpm_file_name) %}
{% set fail_hard = salt['pillar.get']('metering:failHard') %}
{% set proxy_full_url = None %}
{% if salt['pillar.get']('proxy:host') %}
  {% set proxy_host = salt['pillar.get']('proxy:host') %}
  {% set proxy_port = salt['pillar.get']('proxy:port')|string %}
  {% set proxy_protocol = salt['pillar.get']('proxy:protocol') %}
  {% set proxy_url = proxy_protocol + "://" + proxy_host + ":" + proxy_port %}
  {% if salt['pillar.get']('proxy:user') and salt['pillar.get']('proxy:password') %}
    {% set proxy_user = salt['pillar.get']('proxy:user') %}
    {% set proxy_password = salt['pillar.get']('proxy:password') %}
    {% set proxy_full_url =  proxy_protocol + "://" + proxy_user + ":"+ proxy_password + "@" + proxy_host + ":" + proxy_port %}
  {% else %}
    {% set proxy_full_url = proxy_url %}
  {% endif %}
{% endif %}
{% set no_proxy_hosts = salt['pillar.get']('proxy:noProxyHosts') %}

/opt/salt/scripts/metering_package_manager.sh:
  file.managed:
    - user: root
    - group: root
    - mode: 700
    - source: salt://metering/scripts/metering_package_manager.sh
    - template: jinja

execute_metering_binary_upgrade:
  cmd.run:{% if fail_hard %}
    - failhard: True{% endif %}
    - name: "/opt/salt/scripts/metering_package_manager.sh upgrade{% if date_from %} -d {{ date_from }}{% endif %}{% if custom_rpm_url %} -u {{ custom_rpm_url }}{% endif %}{% if not fail_hard %}; exit 0{% endif %}"{% if proxy_full_url %}
    - env:
      - HTTPS_PROXY: {{ proxy_full_url }}{% if no_proxy_hosts %}
      - NO_PROXY: {{ no_proxy_hosts }}{% endif %}{% endif %}

remove_metering_tmp_rpm_file:
  file.absent:
    - name: /tmp/{{ rpm_file_name }}
    - onlyif: test -f /tmp/{{ rpm_file_name }}