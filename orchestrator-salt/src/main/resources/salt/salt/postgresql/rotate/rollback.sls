{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

{% if 'None' != configure_remote_db %}

rollback-db-secrets-remote:
  cmd.run:
    - name: runuser -l postgres -s /bin/bash -c '/opt/salt/scripts/rotate/rotate_db_secrets_remote.sh rollback'

{%- else %}

rollback-db-secrets:
  cmd.run:
    - name: runuser -l postgres -s /bin/bash -c '/opt/salt/scripts/rotate/rotate_db_secrets.sh rollback'

restart-pgsql-if-secret-rollbacked:
  service.running:
    - name: postgresql
    - watch:
      - cmd: rollback-db-secrets

{% endif %}