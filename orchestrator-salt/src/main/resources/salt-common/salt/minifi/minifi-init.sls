{%- from 'minifi/settings.sls' import minifi with context %}
{%- from 'telemetry/settings.sls' import telemetry with context %}
{% set os = salt['grains.get']('os') %}
{% set restart_sleep_time = 1200 %}
{% set cpuarch = salt['grains.get']('cpuarch') %}
{% if minifi.cloudStorageLoggingEnabled %}
{% if not minifi.minifiInstalled %}
{% if os == "RedHat" or os == "CentOS" %}
install_minifi:
  cmd.run:
    - name: rpm -if {{ minifi.minifiRpm }}
{% else %}
install_minifi_warning:
  cmd.run:
    - name: echo "Minifi cannot be installed to {{ os }}"
{% endif %}
{% endif %}

{%- if minifi.is_systemd %}
minifi_systemd_stop:
  service.dead:
    - enable: False
    - name: minifi
    - onlyif: "test -f /etc/nifi-minifi-cpp/config.yml && ! grep -q 'CONFIGURED BY SALT' /etc/nifi-minifi-cpp/config.yml"
{% else %}
minifi_stop:
  cmd.run:
    - name: "/etc/init.d/minifi stop"
    - onlyif: "test -f /etc/nifi-minifi-cpp/config.yml && ! grep -q 'CONFIGURED BY SALT' /etc/nifi-minifi-cpp/config.yml"
{% endif %}

/etc/nifi-minifi-cpp/config.yml:
  file.managed:
    - source: salt://minifi/template/config.yml.j2
    - template: jinja
    - user: root
    - group: root
    - mode: 640
    - context:
        providerPrefix: {{ minifi.providerPrefix }}

{%- if minifi.is_systemd %}
/etc/systemd/system/minifi.d:
  file.directory:
    - name: /etc/systemd/system/minifi.d
    - user: root
    - group: root
    - mode: 740

minifi_systemd_reload_and_run:
  file.copy:
    - name: /etc/systemd/system/minifi.service
    - source: /lib/systemd/system/minifi.service
    - mode: 0644
  module.wait:
    - name: service.systemctl_reload
    - watch:
      - file: /etc/systemd/system/minifi.service
      - file: /etc/nifi-minifi-cpp/config.yml

  service.running:
    - enable: True
    - name: minifi
    - watch:
       - file: /etc/systemd/system/minifi.service
       - file: /etc/nifi-minifi-cpp/config.yml
{% else %}

fs.file-max:
  sysctl.present:
    - value: 100000

minifi_start:
  cmd.run:
    - name: "/etc/init.d/minifi start"
{% endif %}

{% endif %}
