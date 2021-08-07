{%- from 'postgresql/settings.sls' import postgresql with context %}

{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}
{% set roles = salt['grains.get']('roles') %}

{% if 'None' != configure_remote_db and "recover" in roles %}

/opt/salt/scripts/recover_db_remote.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: postgres
    - mode: 750
    - source: salt://postgresql/disaster_recovery/scripts/recover_db_remote.sh
    - template: jinja

recover-services-db-remote:
  cmd.run:
    - name: runuser -l postgres -c '/opt/salt/scripts/recover_db_remote.sh' | tee -a /var/log/recover-services-db-remote.log && [[ 0 -eq ${PIPESTATUS[0]} ]] && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/recover-services-db-remote-executed || exit ${PIPESTATUS[0]}
    - failhard: True
    - onlyif:
      - test ! -f /var/log/recover-services-db-remote-executed
    - require:
      - file: /opt/salt/scripts/recover_db_remote.sh

{% endif %}


