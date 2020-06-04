{%- from 'telemetry/settings.sls' import telemetry with context %}

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

/opt/filecollector/cloud_storage_upload.sh:
   file.managed:
    - source: salt://filecollector/template/cloud_storage_upload.sh.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: '0750'

/opt/filecollector/filecollector-cloud-storage.yaml:
   file.managed:
    - source: salt://filecollector/template/filecollector.yaml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: '0640'

filecollector_cloud_storage_start:
  cmd.run:
    - name: "python3 /opt/filecollector/filecollector.py --config /opt/filecollector/filecollector-cloud-storage.yaml"