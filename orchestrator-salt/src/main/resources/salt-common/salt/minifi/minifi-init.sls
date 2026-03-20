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
    - user: minificpp
    - group: minificpp
    - mode: 640
    - context:
        providerPrefix: {{ minifi.providerPrefix }}

{%- if minifi.minifiPackageVersion is defined and minifi.minifiPackageVersion is not none and minifi.minifiPackageVersion and salt['pkg.version_cmp'](minifi.minifiPackageVersion,'1.26.02-1') >= 0 %}
/etc/nifi-minifi-cpp/minifi.properties.d/minifi-custom.properties:
  file.managed:
    - makedirs: True
    - source: salt://minifi/template/minifi-custom.properties.j2
    - template: jinja
    - user: minificpp
    - group: minificpp
    - mode: 640
    - makedirs: True
{%- else %}

{%- set rendered_content = salt['cp.get_template']('salt://minifi/template/minifi-custom.properties.j2', '/tmp/rendered_temp') -%}

{%- set final_config = salt['cp.get_file_str']('/tmp/rendered_temp') -%}

{%- for line in final_config.splitlines() %}
  {%- if '=' in line and not line.startswith('#') %}
    {%- set key = line.split('=', 1)[0].strip() %}
    {%- set value = line.split('=', 1)[1].strip() %}

update_minifi_property_{{ key }}:
  file.keyvalue:
    - name: /etc/nifi-minifi-cpp/minifi.properties
    - key: "{{ key }}"
    - value: "{{ value }}"
    - separator: '='
    - append_if_not_found: True
  {%- endif %}
{%- endfor %}

cleanup_temp_render:
  file.absent:
    - name: /tmp/rendered_temp
{%- endif %}

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
