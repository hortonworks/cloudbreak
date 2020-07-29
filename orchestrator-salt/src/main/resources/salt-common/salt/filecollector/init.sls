{%- from 'telemetry/settings.sls' import telemetry with context %}
{%- from 'filecollector/settings.sls' import filecollector with context %}
{%- from 'fluent/settings.sls' import fluent with context %}

{% set cdp_telemetry_version = '0.1.0' %}
{% set cdp_telemetry_rpm_location = 'https://cloudera-service-delivery-cache.s3.amazonaws.com/telemetry/cdp-telemetry/'%}
{% set cdp_telemetry_rpm_repo_url = cdp_telemetry_rpm_location + 'cdp_telemetry-' + cdp_telemetry_version + '.x86_64.rpm' %}
{% set cdp_telemetry_package_name = 'cdp-telemetry' %}

{% if filecollector.updatePackage %}
uninstall_telemetry_rpm_if_wrong_version:
  cmd.run:
    - name: rpm -e {{ cdp_telemetry_package_name }}
    - onlyif: rpm -qa {{ cdp_telemetry_package_name }} | grep -v {{ cdp_telemetry_version }}
    - failhard: True
install_telemetry_rpm_manually:
  cmd.run:
    - name: "rpm -i {{ cdp_telemetry_rpm_repo_url }}"
    - onlyif: "! rpm -q {{ cdp_telemetry_package_name }}"
    - failhard: True
{% else %}
fail_if_telemetry_rpm_is_not_installed:
  cmd.run:
    - name: echo "Cdp telemetry is not installed, it is required for using filecollector"; exit 1
    - onlyif: "! rpm -q {{ cdp_telemetry_package_name }}"
    - failhard: True
{% endif %}

/var/lib/filecollector:
  file.directory:
    - name: /var/lib/filecollector
    - user: "root"
    - group: "root"
    - mode: 750
    - failhard: True

/opt/cdp-telemetry/conf/filecollector-collect.yaml:
   file.managed:
    - source: salt://filecollector/template/filecollector.yaml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: '0640'
    - failhard: True
    - context:
        destination: "LOCAL"

{% if fluent.dbusClusterLogsCollection %}
/opt/cdp-telemetry/conf/filecollector-eng.yaml:
   file.managed:
    - source: salt://filecollector/template/filecollector.yaml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: '0640'
    - failhard: True
    - context:
        destination: "ENG"
{% endif %}

/opt/cdp-telemetry/conf/bundle_info.json:
   file.managed:
    - source: salt://filecollector/template/bundle_info.json.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: '0750'
    - failhard: True