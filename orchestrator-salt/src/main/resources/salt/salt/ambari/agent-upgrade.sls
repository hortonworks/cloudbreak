{%- from 'ambari/settings.sls' import ambari with context %}

include:
  - ambari.repo

create_ambari_upgrade_log_dir:
  file.directory:
    - name: /var/log/ambari-upgrade

stop-ambari-agent:
  service.dead:
    - name: ambari-agent

upgrade-ambari-agent:
  pkg.latest:
    - name: ambari-agent
    - require:
      - sls: ambari.repo
    - version: {{ ambari.version }}