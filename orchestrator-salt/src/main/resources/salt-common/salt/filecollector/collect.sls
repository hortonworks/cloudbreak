{%- from 'filecollector/settings.sls' import filecollector with context %}

{% set extra_params="" %}
{% if filecollector.startTime %}
  {% set startTimeStr = filecollector.startTime|string %}
  {% set extra_params = extra_params + " --start-time " + startTimeStr %}
{% endif %}
{% if filecollector.endTime %}
  {% set endTimeStr = filecollector.endTime|string %}
  {% set extra_params = extra_params + " --end-time " + endTimeStr %}
{% endif %}
{% if filecollector.labelFilter %}
  {% set labels_str = ",".join(filecollector.labelFilter) %}
  {% set extra_params = extra_params + " --labels " + labels_str %}
{% endif %}

filecollector_collect_start:
  cmd.run:
{% if filecollector.destination in ["CLOUD_STORAGE", "LOCAL", "SUPPORT"] %}
    - name: "python3 /opt/filecollector/filecollector.py --config /opt/filecollector/filecollector-collect.yaml {{ extra_params }}"
{% elif filecollector.destination == "ENG" %}
    - name: "python3 /opt/filecollector/filecollector.py --config /opt/filecollector/filecollector-eng.yaml {{ extra_params }}"
{% else %}
    - name: echo "Not supported destination: {{ filecollector.destination }}"
{% endif %}
    - env:
        - LC_ALL: "en_US.utf8"