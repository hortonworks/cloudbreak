{%- from 'telemetry/settings.sls' import telemetry with context %}
{%- from 'filecollector/settings.sls' import filecollector with context %}
{%- from 'fluent/settings.sls' import fluent with context %}
{%- from 'databus/settings.sls' import databus with context %}

{% set cdp_telemetry_version = '0.3.3' %}
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
{% if filecollector.proxyUrl %}
    - env:{% if filecollector.proxyProtocol == "https" %}
      - https_proxy: {{ filecollector.proxyUrl }}{% else %}
      - http_proxy: {{ filecollector.proxyUrl }}{% endif %}{% endif %}
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

/opt/cdp-telemetry/conf/diagnostics_request.json:
   file.managed:
    - source: salt://filecollector/template/diagnostics_request.json.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: '0750'
    - failhard: True

/opt/cdp-telemetry/conf/cdp-doctor-commands.yaml:
   file.managed:
    - source: salt://filecollector/template/cdp-doctor-commands.yaml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: '0750'
    - failhard: True

{% if not filecollector.skipValidation and filecollector.destination == "CLOUD_STORAGE" %}
create_test_cloud_storage_file:
  cmd.run:
    - name: echo testcloudstorage > /tmp/.test_cloud_storage_upload.txt
    - failhard: True

{% if "s3" in filecollector.cloudStorageUploadParams %}
test_s3_put:
  cmd.run:
    - name: "cdp-telemetry storage {{ filecollector.testCloudStorageUploadParams }}"
    - failhard: True
{% elif "abfs" in filecollector.cloudStorageUploadParams %}
test_abfs_put:
  cmd.run:
    - name: "cdp-telemetry storage {{ filecollector.testCloudStorageUploadParams }}"
    - failhard: True
{% endif %}

delete_test_cloud_storage_file:
  cmd.run:
    - name: rm -rf /tmp/.test_cloud_storage_upload.txt
{% elif not filecollector.skipValidation and filecollector.destination == "ENG" and fluent.dbusClusterLogsCollection and databus.endpoint %}
check_dbus_connection:
  cmd.run:
    - name: "cdp-telemetry utils check-connection --url {{ databus.endpoint }}"
    - failhard: True
check_td_agent_running_systemctl:
  cmd.run:
    - name: "systemctl is-active --quiet td-agent"
    - failhard: True
{% elif not filecollector.skipValidation and filecollector.destination == "SUPPORT" %}
check_support_dbus_connection:
  cmd.run:
    - name: "cdp-telemetry utils check-connection --url {{ filecollector.dbusUrl }}"
    - failhard: True{% if filecollector.proxyUrl %}
    - env: {% if filecollector.proxyProtocol == "https" %}
       - HTTPS_PROXY: {{ filecollector.proxyUrl }}{% else %}
       - HTTP_PROXY: {{ filecollector.proxyUrl }}{% endif %}{% endif %}
{% endif %}