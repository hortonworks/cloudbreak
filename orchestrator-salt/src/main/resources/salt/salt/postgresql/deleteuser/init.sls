{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

{% if 'None' != configure_remote_db %}

/opt/salt/scripts/remove_user_from_db_remote.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: postgres
    - mode: 750
    - source: salt://postgresql/deleteuser/scripts/remove_user_from_db_remote.sh
    - template: jinja
    - replace: True

remove_user_from_db_remote:
  cmd.run:
    - name: runuser -l postgres -s /bin/bash -c '/opt/salt/scripts/remove_user_from_db_remote.sh' 2>&1 | tee -a /var/log/remove_user_from_db_remote.log && exit ${PIPESTATUS[0]}
    - require:
      - file: /opt/salt/scripts/remove_user_from_db_remote.sh
{% endif %}