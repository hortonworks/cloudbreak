{%- from 'filecollector/settings.sls' import filecollector with context %}

run_filecollector_cleanup_all:
  cmd.run:
    - name: "sh /opt/filecollector/cleanup.sh {{ filecollector.destination }}"