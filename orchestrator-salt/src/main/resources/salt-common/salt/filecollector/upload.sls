{%- from 'filecollector/settings.sls' import filecollector with context %}
{% if filecollector.mode == "CLOUDERA_MANAGER" %}
move_support_bundle_to_filecollector:
  cmd.run:
    - name: find /tmp -name "*-scm-command-result-data*.zip" -printf "%p %f\n" | awk 'BEGIN {FS = OFS = " "} {split($2,a,"-"); ; printf "%s /var/lib/filecollector/CM_DIAGNOSTICS_BUNDLE-%s.zip\n", $1, a[1]}' | xargs -L 1 mv
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
    - name: "cdp-telemetry databus upload -p {{ filecollector.compressedFilePattern }} -c /opt/cdp-telemetry/conf/support_bundle_databus.conf --stream {{ filecollector.supportBundleDbusStreamName }} --url {{ filecollector.dbusUrl }}"
    - failhard: True
    - env:
        - CDP_TELEMETRY_LOGGER_FILENAME: "upload.log"{% if filecollector.proxyUrl %}{% if filecollector.proxyProtocol == "https" %}
        - HTTPS_PROXY: {{ filecollector.proxyUrl }}{% else %}
        - HTTP_PROXY: {{ filecollector.proxyUrl }}{% endif %}{% endif %}
{% endif %}