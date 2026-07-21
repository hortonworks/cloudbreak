/opt/salt/scripts/generate-livy-local-jars.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - source: salt://livy/scripts/generate-livy-local-jars.sh

generate-livy-local-jars:
  cmd.run:
    - name: /opt/salt/scripts/generate-livy-local-jars.sh >> /var/log/generate-livy-local-jars.log
    - require:
      - file: /opt/salt/scripts/generate-livy-local-jars.sh
