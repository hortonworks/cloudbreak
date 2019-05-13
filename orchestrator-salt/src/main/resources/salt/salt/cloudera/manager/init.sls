{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}

install-cloudera-manager-server:
  pkg.installed:
    - pkgs:
      - cloudera-manager-daemons
      - cloudera-manager-agent
      - cloudera-manager-server

{% if salt['pillar.get']('ldap', None) != None and salt['pillar.get']('ldap:local', None) == None %}

/etc/cloudera-scm-server/ldap.settings:
  file.managed:
    - makedirs: True
    - source: salt://cloudera/manager/ldap/ldap.settings
    - template: jinja
    - context:
      ldap: {{ cloudera_manager.ldap }}
    - mode: 744

cloudera_manager_setup_ldap:
  file.replace:
    - name: /etc/default/cloudera-scm-server
    - pattern: "CMF_SERVER_ARGS=.*"
    - repl: CMF_SERVER_ARGS="-i /etc/cloudera-scm-server/ldap.settings"
    - unless: grep "CMF_SERVER_ARGS=\"-i /etc/cloudera-scm-server/ldap.settings\"" /etc/default/cloudera-scm-server

{% endif %}

{% if salt['pillar.get']('cloudera-manager:license', None) != None %}

/etc/cloudera-scm-server/license.txt:
  file.managed:
    - source: salt://cloudera/manager/license.txt
    - template: jinja

{% endif %}
