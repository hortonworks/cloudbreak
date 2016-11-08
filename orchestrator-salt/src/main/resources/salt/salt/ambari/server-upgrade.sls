{%- from 'ambari/settings.sls' import ambari with context %}

include:
  - ambari.repo

stop-ambari-server:
  service.dead:
    - name: ambari-server

/opt/ambari-server/ambari-upgrade.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/scripts/ambari-upgrade.sh
    - template: jinja
    - context:
      ambari: {{ ambari }}
    - mode: 744

create_upgrade_history:
  cmd.run:
    - name: touch /var/ambari-upgrade.history
    - unless: ls -1 /var/ambari-upgrade.history

upgrade-ambari-server:
  pkg.installed:
    - name: ambari-server
    - require:
      - sls: ambari.repo
    - version: {{ ambari.version }}

upgrade_ambari_server_db:
  cmd.run:
    - name: /opt/ambari-server/ambari-upgrade.sh 2>&1 | tee -a /var/log/ambari-upgrade/ambari-upgrade.log && exit ${PIPESTATUS[0]}
    - unless: cat /var/ambari-upgrade.history | grep '{{ ambari.version }}' &>/dev/null