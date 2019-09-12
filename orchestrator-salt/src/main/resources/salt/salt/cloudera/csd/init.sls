/opt/salt/scripts/csd-downloader.sh:
  file.managed:
    - makedirs: True
    - source: salt://cloudera/csd/csd-downloader.sh
    - template: jinja
    - mode: 755

download-csd:
  cmd.run:
    - name: /opt/salt/scripts/csd-downloader.sh
    - require:
      - file: /opt/salt/scripts/csd-downloader.sh
    - shell: /bin/bash
    - unless: test -f /var/csd_downloaded
