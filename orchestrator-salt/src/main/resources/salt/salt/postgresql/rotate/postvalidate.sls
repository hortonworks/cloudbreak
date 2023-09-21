{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

{% if 'None' != configure_remote_db %}

/opt/salt/scripts/rotate/rotate_db_secrets_remote.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: postgres
    - mode: 750
    - source: salt://postgresql/rotate/scripts/rotate_db_secrets_remote.sh
    - template: jinja

postvalidate-db-secrets-remote:
  cmd.run:
    - name: runuser -l postgres -s /bin/bash -c '/opt/salt/scripts/rotate/rotate_db_secrets_remote.sh postvalidate' 2>&1 | tee -a /var/log/postgresql_rotate_postvalidate.log && exit ${PIPESTATUS[0]}
    - require:
      - file: /opt/salt/scripts/rotate/rotate_db_secrets_remote.sh

{%- else %}

/opt/salt/scripts/rotate/rotate_db_secrets.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - user: root
    - group: postgres
    - source: salt://postgresql/rotate/scripts/rotate_db_secrets.sh
    - template: jinja

postvalidate-db-secrets:
  cmd.run:
    - name: runuser -l postgres -s /bin/bash -c '/opt/salt/scripts/rotate/rotate_db_secrets.sh postvalidate' 2>&1 | tee -a /var/log/postgresql_rotate_postvalidate.log && exit ${PIPESTATUS[0]}
    - require:
      - file: /opt/salt/scripts/rotate/rotate_db_secrets.sh

{% endif %}