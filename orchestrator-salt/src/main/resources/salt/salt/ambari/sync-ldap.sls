{%- from 'ambari/settings.sls' import ambari with context %}

{% if not ambari.is_local_ldap %}

/opt/ambari-server/sync-ldap.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/scripts/sync-ldap.sh
    - template: jinja
    - context:
      ambari: {{ ambari }}
    - mode: 744

sync_ldap:
  cmd.run:
    - name: /opt/ambari-server/sync-ldap.sh
    - shell: /bin/bash
    - unless: test -f /var/ldap_sync_success

{% endif %}