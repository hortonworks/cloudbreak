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
{% endif %}