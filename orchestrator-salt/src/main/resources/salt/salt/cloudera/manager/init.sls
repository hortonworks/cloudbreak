{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}

install-cloudera-manager-server:
  pkg.installed:
    - pkgs:
      - cloudera-manager-daemons
      - cloudera-manager-agent
      - cloudera-manager-server

{% if salt['pillar.get']('ldap', None) != None and salt['pillar.get']('ldap:local', None) == None %}

add_ldap_settings_to_cm:
  file.append:
    - name: /etc/cloudera-scm-server/cm.settings
    - makedirs: True
    - template: jinja
    - source: salt://cloudera/manager/ldap/ldap.settings
    - context:
        ldap: {{ cloudera_manager.ldap }}
    - unless: grep "AUTH_BACKEND_ORDER" /etc/cloudera-scm-server/cm.settings

cloudera_manager_setup_ldap:
  file.replace:
    - name: /etc/default/cloudera-scm-server
    - pattern: "CMF_SERVER_ARGS=.*"
    - repl: CMF_SERVER_ARGS="-i /etc/cloudera-scm-server/cm.settings"
    - unless: grep "CMF_SERVER_ARGS=\"-i /etc/cloudera-scm-server/cm.settings\"" /etc/default/cloudera-scm-server

{% endif %}

{% if salt['pillar.get']('cloudera-manager:license', None) != None %}

/etc/cloudera-scm-server/license.txt:
  file.managed:
    - source: salt://cloudera/manager/license.txt
    - template: jinja

{% endif %}

cloudera_manager_set_parcel_validation:
  file.replace:
    - name: /opt/cloudera/cm/bin/cm-server
    - pattern: "CMF_OPTS -server"
    - repl: "CMF_OPTS -server\"\nCMF_OPTS=\"$CMF_OPTS -Dcom.cloudera.parcel.VALIDATE_PARCELS_HASH=false"
    - unless: grep "VALIDATE_PARCELS_HASH=false" /opt/cloudera/cm/bin/cm-server
