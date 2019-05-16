{%- from 'gateway/settings.sls' import gateway with context %}

add_knox_settings_to_cm:
  file.append:
    - name: /etc/cloudera-scm-server/cm.settings
    - makedirs: True
    - template: jinja
    - source: salt://gateway/config/cm/knox.settings.j2
    - unless: grep "PROXYUSER_KNOX_GROUPS" /etc/cloudera-scm-server/cm.settings

cloudera_manager_setup_knox:
  file.replace:
    - name: /etc/default/cloudera-scm-server
    - pattern: "CMF_SERVER_ARGS=.*"
    - repl: CMF_SERVER_ARGS="-i /etc/cloudera-scm-server/cm.settings"
    - unless: grep "CMF_SERVER_ARGS=\"-i /etc/cloudera-scm-server/cm.settings\"" /etc/default/cloudera-scm-server

/var/lib/knox/gateway/conf/topologies:
  file.directory:
    - mode: 777
    - makedirs: True

/var/lib/knox/gateway/conf/topologies/admin.xml:
  file.managed:
    - source: salt://gateway/config/cm/admin.xml.j2
    - template: jinja
    - mode: 777
    - context:
        ldap: {{ gateway.ldap }}

/var/lib/knox/gateway/conf/topologies/manager.xml:
  file.managed:
    - source: salt://gateway/config/cm/manager.xml.j2
    - template: jinja
    - mode: 777
    - context:
        ldap: {{ gateway.ldap }}

/var/lib/knox/gateway/conf/topologies/knoxsso.xml:
  file.managed:
    - source: salt://gateway/config/cm/knoxsso.xml.j2
    - template: jinja
    - mode: 777

/var/lib/knox/gateway/conf/topologies/sandbox.xml:
  file.managed:
    - source: salt://gateway/config/cm/sandbox.xml.j2
    - template: jinja
    - mode: 777