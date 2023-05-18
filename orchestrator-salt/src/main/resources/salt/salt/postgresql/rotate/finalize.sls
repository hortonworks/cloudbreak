{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

{% if 'None' != configure_remote_db %}

finalize-db-secrets-remote:
  cmd.run:
    - name: runuser -l postgres -s /bin/bash -c '/opt/salt/scripts/rotate/rotate_db_secrets_remote.sh finalize'

{%- else %}

finalize-db-secrets:
  cmd.run:
    - name: runuser -l postgres -s /bin/bash -c '/opt/salt/scripts/rotate/rotate_db_secrets.sh finalize'

restart-pgsql-if-rotation-finalized:
  service.running:
    - name: postgresql
    - watch:
      - cmd: finalize-db-secrets

{% endif %}