{%- from 'postgresql/settings.sls' import postgresql with context %}

{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

{% if 'None' != configure_remote_db %}

recover-services-db-remote-required:
  cmd.run:
    - name: echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/recover-services-db-remote-required
    - unless: test -f /var/log/recover-services-db-remote-required

{% endif %}