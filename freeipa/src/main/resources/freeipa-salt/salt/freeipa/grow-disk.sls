/opt/salt/scripts/grow_disk.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/grow_disk.sh

run-growdisk-script:
  cmd.run:
    - name: /opt/salt/scripts/grow_disk.sh && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/grow_disk-executed
    - env:
        - CLOUD_PLATFORM: {{salt['pillar.get']('platform')}}
    - failhard: True
    - unless: test -f /var/log/grow_disk-executed
    - require:
      - file: /opt/salt/scripts/grow_disk.sh
