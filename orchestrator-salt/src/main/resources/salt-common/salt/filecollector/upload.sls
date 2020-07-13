{%- from 'filecollector/settings.sls' import filecollector with context %}

{% if filecollector.destination == "CLOUD_STORAGE" %}
filecollector_upload_to_cloud_storage:
  cmd.run:
    - name: "sh /opt/filecollector/cloud_storage_upload.sh"
{% endif %}