/opt/salt/scripts/check_kraft_acls.sh:
  file.managed:
    - source:
        - salt://kafka/scripts/check_kraft_acls.sh
    - makedirs: True
    - mode: 755

check_kraft_acls:
  cmd.run:
    - name: /opt/salt/scripts/check_kraft_acls.sh
    - failhard: True
    - require:
        - file: /opt/salt/scripts/check_kraft_acls.sh
