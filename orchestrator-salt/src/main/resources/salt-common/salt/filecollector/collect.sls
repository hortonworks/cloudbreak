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
  {% set extra_params = extra_params + " --label " + " --label ".join(filecollector.labelFilter) %}
{% endif %}

run_cdp_doctor:
  cmd.run:
    - name: "cdp-telemetry doctor commands create -c /opt/cdp-telemetry/conf/cdp-doctor-commands.yaml"

{% if filecollector.includeSarOutput %}
create_sar_output_dir:
  file.directory:
    - name: /tmp/sar_output
    - mode: 740

generate_plain_text_sar_output:
  cmd.run:
    - name: "cd /var/log/sa/ && find . -type f -printf '%f\n' | xargs -I {} bash -c 'sar -A -f {} > /tmp/sar_output/{}.txt'; exit 0"
    - onlyif: test -d /var/log/sa
{% endif %}

{% if filecollector.includeNginxReport %}
generate_nginx_report:
  cmd.run:
    - name: "goaccess /var/log/nginx/access.log -o /tmp/nginx_report.html --log-format=COMBINED; exit 0"
    - onlyif: test -f /var/log/nginx/access.log
    - env:
        - HOME: "/tmp/.goaccess"
{% endif %}

{%- if filecollector.includeSeLinuxReport %}
/tmp/generate_selinux_report.sh:
  file.managed:
    - source: salt://{{ slspath }}/scripts/generate_selinux_report.sh
    - mode: 750
    - user: root
    - group: root

generate_selinux_report:
  cmd.run:
    - name: "/tmp/generate_selinux_report.sh"
{%- endif %}

filecollector_collect_start:
  cmd.run:
{% if filecollector.destination in ["CLOUD_STORAGE", "LOCAL", "SUPPORT"] %}
    - name: "cdp-telemetry filecollector collect --config /opt/cdp-telemetry/conf/filecollector-collect.yaml {{ extra_params }}"
{% else %}
    - name: 'echo Not supported destination: {{ filecollector.destination }}'
{% endif %}
    - failhard: True
    - env:
        - LC_ALL: "en_US.utf8"
        - CDP_TELEMETRY_LOGGER_FILENAME: "filecollector.log"

{% if filecollector.includeSarOutput %}
remove_sar_output_dir:
  file.absent:
    - name: /tmp/sar_output/
    - clean: True
{% endif %}