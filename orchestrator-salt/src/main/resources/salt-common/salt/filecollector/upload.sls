{%- from 'telemetry/settings.sls' import telemetry with context %}
{%- from 'filecollector/settings.sls' import filecollector with context %}
{% if filecollector.mode == "CLOUDERA_MANAGER" %}
move_support_bundle_to_filecollector:
  cmd.run:
    - names:
      - find /tmp -name "*-scm-command-result-data*.zip" -printf "%p %f\n" | awk 'BEGIN {FS = OFS = " "} {split($2,a,"-"); ; printf "%s /var/lib/filecollector/CM_DIAGNOSTICS_BUNDLE-%s.zip\n", $1, a[1]}' | xargs -L 1 mv
      - chmod -R 644 /var/lib/filecollector/CM_DIAGNOSTICS_BUNDLE*
{% endif %}

{% if filecollector.destination == "CLOUD_STORAGE" %}
filecollector_upload_to_cloud_storage:
  cmd.run:
    - name: "cdp-telemetry storage {{ filecollector.cloudStorageUploadParams }}"
    - failhard: True
    - env:
        - CDP_TELEMETRY_LOGGER_FILENAME: "upload.log"
{% endif %}

{% if filecollector.destination == "SUPPORT" %}

{% if filecollector.updatePackage or telemetry.cdpTelemetryVersion > 3 %}
    {% set extra_dbus_header_file_param = "-e /opt/cdp-telemetry/conf/extra-dbus-headers.yaml" %}
{% else %}
    {% set extra_dbus_header_file_param = "" %}
{% endif %}

/opt/cdp-telemetry/conf/support_bundle_databus.conf:
   file.managed:
    - source: salt://filecollector/template/support_bundle_databus.conf.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: '0600'
    - failhard: True

filecollector_upload_to_support:
  cmd.run:
    - name: "cdp-telemetry databus upload -p '{{ filecollector.compressedFilePattern }}' -c /opt/cdp-telemetry/conf/support_bundle_databus.conf --stream {{ filecollector.supportBundleDbusStreamName }} --url {{ filecollector.dbusUrl }} {{ extra_dbus_header_file_param }} {{ filecollector.supportBundleDbusHeaders}}"
    - failhard: True
    - env:
        - CDP_TELEMETRY_LOGGER_FILENAME: "upload.log"{% if filecollector.proxyUrl %}{% if filecollector.proxyProtocol == "https" %}
        - HTTPS_PROXY: {{ filecollector.proxyUrl }}{% else %}
        - HTTP_PROXY: {{ filecollector.proxyUrl }}{% if filecollector.noProxyHosts and telemetry.cdpTelemetryVersion > 8 %}
        - NO_PROXY: {{ filecollector.noProxyHosts }}{% endif %}{% endif %}{% endif %}
{% endif %}