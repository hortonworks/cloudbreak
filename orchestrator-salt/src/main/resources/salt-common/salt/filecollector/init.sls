{%- from 'telemetry/settings.sls' import telemetry with context %}
{%- from 'filecollector/settings.sls' import filecollector with context %}
{%- from 'fluent/settings.sls' import fluent with context %}
{%- from 'databus/settings.sls' import databus with context %}

{% if filecollector.updatePackage %}
include:
  - telemetry.upgrade
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

{% if (filecollector.updatePackage or telemetry.cdpTelemetryVersion > 3) and filecollector.destination == "SUPPORT" %}
/opt/cdp-telemetry/conf/extra-dbus-headers.yaml:
   file.managed:
    - source: salt://filecollector/template/extra-dbus-headers.yaml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: '0750'
    - failhard: True
{% endif %}

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
    - name: "curl {{ telemetry.databusCurlConnectOpts }} -s -k {{ databus.endpoint }} > /dev/null"
    - failhard: True{% if filecollector.proxyUrl %}
    - env:
       - https_proxy: {{ filecollector.proxyUrl }}{% if filecollector.noProxyHosts and telemetry.cdpTelemetryVersion > 8 %}
       - no_proxy: {{ filecollector.noProxyHosts }}{% endif %}{% endif %}
check_logging_agent_running_systemctl:
  cmd.run:
    - name: "systemctl is-active --quiet td-agent || systemctl is-active --quiet cdp-logging-agent"
    - failhard: True
{% elif filecollector.dbusUrl and filecollector.destination == "SUPPORT" %}
check_support_dbus_connection:
  cmd.run:
    - name: "curl {{ telemetry.databusCurlConnectOpts }} -s -k {{ filecollector.dbusUrl }} > /dev/null"
    - failhard: True{% if filecollector.proxyUrl %}
    - env: {% if filecollector.proxyProtocol == "https" %}
       - https_proxy: {{ filecollector.proxyUrl }}{% else %}
       - http_proxy: {{ filecollector.proxyUrl }}{% endif %}{% if filecollector.noProxyHosts and telemetry.cdpTelemetryVersion > 8 %}
       - no_proxy: {{ filecollector.noProxyHosts }}{% endif %}{% endif %}
{% if filecollector.dbusS3Url %}
check_support_dbus_s3_connection:
  cmd.run:
    - name: "curl {{ telemetry.databusCurlConnectOpts }} -s -k {{ filecollector.dbusS3Url }} > /dev/null"
    - failhard: True{% if filecollector.proxyUrl %}
    - env: {% if filecollector.proxyProtocol == "https" %}
       - https_proxy: {{ filecollector.proxyUrl }}{% else %}
       - http_proxy: {{ filecollector.proxyUrl }}{% endif %}{% if filecollector.noProxyHosts and telemetry.cdpTelemetryVersion > 8 %}
       - no_proxy: {{ filecollector.noProxyHosts }}{% endif %}{% endif %}
{% endif %}
{% endif %}

{% if not filecollector.skipWorkspaceCleanupOnStartup %}
filecollector_clean_dirs_at_startup:
  cmd.run:
    - names:
{% if filecollector.destination == "LOCAL" %}
        - cdp-telemetry utils clean -d /var/lib/filecollector/ -p "tmp/**"
{% elif filecollector.destination == "ENG" %}
        - cdp-telemetry utils clean -d /var/lib/filecollector/
{% else %}
        - cdp-telemetry utils clean -d /var/lib/filecollector/ -p "tmp/**"
        - cdp-telemetry utils clean -d /var/lib/filecollector/ -p "*.gz"
{% if filecollector.mode == "CLOUDERA_MANAGER" %}
        - cdp-telemetry utils clean -d /var/lib/filecollector/ -p "*.zip"{% endif %}
{% endif %}
{% endif %}