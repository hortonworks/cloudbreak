{%- from 'telemetry/settings.sls' import telemetry with context %}
{%- from 'filecollector/settings.sls' import filecollector with context %}
{%- from 'fluent/settings.sls' import fluent with context %}

install_fluent_logger:
  cmd.run:
    - name: pip install fluent-logger>=0.9.6 --ignore-installed
    - unless: pip list --no-index | grep -E 'fluent-logger'

install_yaml:
  cmd.run:
    - name: pip install PyYAML --ignore-installed
    - unless: pip list --no-index | grep -E 'PyYAML'

install_pid:
  cmd.run:
    - name: pip install pid --ignore-installed
    - unless: pip list --no-index | grep -E 'pid'

/opt/filecollector:
  file.directory:
    - name: /opt/filecollector
    - user: "root"
    - group: "root"
    - mode: 740
    - recurse:
      - user
      - group
      - mode

/var/lib/filecollector:
  file.directory:
    - name: /var/lib/filecollector
    - user: "root"
    - group: "root"
    - mode: 740

/opt/filecollector/filecollector.py:
   file.managed:
    - source: salt://filecollector/scripts/filecollector.py
    - user: "root"
    - group: "root"
    - mode: '0750'

/opt/filecollector/filecollector-collect.yaml:
   file.managed:
    - source: salt://filecollector/template/filecollector.yaml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: '0640'
    - context:
        destination: "LOCAL"

{% if fluent.dbusClusterLogsCollection %}
/opt/filecollector/filecollector-eng.yaml:
   file.managed:
    - source: salt://filecollector/template/filecollector.yaml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: '0640'
    - context:
        destination: "ENG"
{% endif %}

/opt/filecollector/bundle_info.json:
   file.managed:
    - source: salt://filecollector/template/bundle_info.json.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: '0750'

/opt/filecollector/cloud_storage_upload.sh:
   file.managed:
    - source: salt://filecollector/template/cloud_storage_upload.sh.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: '0750'

/opt/filecollector/cleanup.sh:
   file.managed:
    - source: salt://filecollector/scripts/cleanup.sh
    - user: "root"
    - group: "root"
    - mode: '0750'