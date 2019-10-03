{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}
{%- from 'metadata/settings.sls' import metadata with context %}

install-cloudera-manager-server:
  pkg.installed:
    - pkgs:
      - cloudera-manager-daemons
      - cloudera-manager-agent
      - cloudera-manager-server

/etc/cloudera-scm-server/cm.settings:
  file.managed:
    - contents: # Created by CB Saltstack
    - user: cloudera-scm
    - group: cloudera-scm
    - mode: 600

add_public_cloud_settings_to_cm:
  file.append:
    - name: /etc/cloudera-scm-server/cm.settings
    - text: |
        # CDP ENV settings
        setsettings CDP_ENV PUBLIC_CLOUD
    - unless: grep "CDP_ENV" /etc/cloudera-scm-server/cm.settings

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

{% if salt['pillar.get']('cloudera-manager:cme_enabled') %}
cmf_ff_cme_enabled:
  file.blockreplace:
    - name: /etc/default/cloudera-scm-server
    - marker_start: "# BLOCK TOP : salt managed zone : please do not edit"
    - marker_end: "# BLOCK BOTTOM : end of salt managed zone --"
    - content: "export CMF_FF_CME=true"
    - show_changes: True
    - append_if_not_found: True
{% endif %}

cloudera_manager_set_parcel_validation:
  file.replace:
    - name: /opt/cloudera/cm/bin/cm-server
    - pattern: "CMF_OPTS -server"
    - repl: "CMF_OPTS -server\"\nCMF_OPTS=\"$CMF_OPTS -Dcom.cloudera.parcel.VALIDATE_PARCELS_HASH=false"
    - unless: grep "VALIDATE_PARCELS_HASH=false" /opt/cloudera/cm/bin/cm-server

{% if cloudera_manager.communication.autotls_enabled == True %}

/opt/salt/scripts/cm-setup-autotls.sh:
  file.managed:
    - makedirs: True
    - source: salt://cloudera/manager/scripts/setup-autotls.sh
    - template: jinja
    - mode: 700
    - context:
        cm_keytab: {{ cloudera_manager.cm_keytab }}
        server_address: {{ metadata.server_address }}

run_autotls_setup:
  cmd.run:
    - name: /opt/salt/scripts/cm-setup-autotls.sh
    - require:
      - file: /opt/salt/scripts/cm-setup-autotls.sh
    - env:
        - AUTO_TLS_KEYSTORE_PASSWORD: {{salt['pillar.get']('cloudera-manager:autotls:keystore_password')}}
        - AUTO_TLS_TRUSTSTORE_PASSWORD: {{salt['pillar.get']('cloudera-manager:autotls:truststore_password')}}
    - unless: test -f /etc/cloudera-scm-server/certs/autotls_setup_success

copy_autotls_setup_to_cm_settings:
  cmd.run:
    - name: >
        echo "# Auto-tls related configurations" >> /etc/cloudera-scm-server/cm.settings;
        cat /etc/cloudera-scm-server/certs/auto-tls.init.txt >> /etc/cloudera-scm-server/cm.settings
    - require:
      - cmd: run_autotls_setup
    - unless: grep "# Auto-tls related configurations" /etc/cloudera-scm-server/cm.settings

/opt/salt/scripts/cm_generate_agent_tokens.sh:
  file.managed:
    - makedirs: True
    - source: salt://cloudera/manager/scripts/generate_agent_tokens.sh
    - template: jinja
    - mode: 700

run_generate_agent_tokens:
  cmd.run:
    - name: /opt/salt/scripts/cm_generate_agent_tokens.sh
    - require:
        - file: /opt/salt/scripts/cm_generate_agent_tokens.sh
        - cmd: run_autotls_setup
{% endif %}
