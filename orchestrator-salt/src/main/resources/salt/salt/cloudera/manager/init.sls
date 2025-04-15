{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}
{%- from 'metadata/settings.sls' import metadata with context %}

install-cloudera-manager-server:
  pkg.installed:
    - failhard: True
    - pkgs:
      - cloudera-manager-daemons
      - cloudera-manager-agent
      - cloudera-manager-server
    - unless:
      - rpm -q cloudera-manager-daemons cloudera-manager-agent cloudera-manager-server

/etc/cloudera-scm-server/cm.settings:
  file.managed:
    - user: cloudera-scm
    - group: cloudera-scm
    - mode: 600
    - replace: False

setup_cm_heartbeat:
  file.append:
    - name: /etc/cloudera-scm-server/cm.settings
    - text: setsettings HEARTBEAT_INTERVAL {{ cloudera_manager.settings.heartbeat_interval }}
    - unless: grep "HEARTBEAT_INTERVAL" /etc/cloudera-scm-server/cm.settings

setup_missed_cm_heartbeat:
  file.append:
    - name: /etc/cloudera-scm-server/cm.settings
    - text: setsettings MISSED_HB_BAD {{ cloudera_manager.settings.missed_heartbeat_interval }}
    - unless: grep "MISSED_HB_BAD" /etc/cloudera-scm-server/cm.settings

setup_tls_chipher:
  file.append:
    - name: /etc/cloudera-scm-server/cm.settings
    - text: setsettings CMF_OVERRIDE_TLS_CIPHERS TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256:TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256:TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384:TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384:TLS_DHE_RSA_WITH_AES_128_GCM_SHA256:TLS_DHE_RSA_WITH_AES_256_GCM_SHA384:TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256:TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA:TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA:TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384:TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA:TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA:TLS_DHE_RSA_WITH_AES_128_CBC_SHA256:TLS_DHE_RSA_WITH_AES_128_CBC_SHA:TLS_DHE_RSA_WITH_AES_256_CBC_SHA256:TLS_DHE_RSA_WITH_AES_256_CBC_SHA:TLS_RSA_WITH_AES_128_CBC_SHA:TLS_RSA_WITH_AES_256_CBC_SHA
    - unless: grep "CMF_OVERRIDE_TLS_CIPHERS" /etc/cloudera-scm-server/cm.settings

{% if salt['pillar.get']('cluster:gov_cloud', False) == True %}

setup_fips_mode:
  file.append:
    - name: /etc/default/cloudera-scm-server
    - text: "export CMF_JAVA_OPTS=\"${CMF_JAVA_OPTS} -Dcom.cloudera.cmf.fipsMode=true -Dcom.safelogic.cryptocomply.fips.approved_only=true -Dorg.bouncycastle.jsse.client.assumeOriginalHostName=true\""
    - unless: grep "export CMF_JAVA_OPTS=\"\${CMF_JAVA_OPTS} -Dcom.cloudera.cmf.fipsMode=true -Dcom.safelogic.cryptocomply.fips.approved_only=true -Dorg.bouncycastle.jsse.client.assumeOriginalHostName=true\"" /etc/default/cloudera-scm-server

{% endif %}

{% set cloud_type = 'PUBLIC_CLOUD' %}
{% if salt['pillar.get']('cluster:hybridEnabled', False) == True %}
  {% set cloud_type = 'PRIVATE_CLOUD' %}
{% endif %}

add_settings_file_to_cfm_server_args:
  file.replace:
    - name: /etc/default/cloudera-scm-server
    - pattern: "CMF_SERVER_ARGS=.*"
{% if salt['pillar.get']('cloudera-manager:settings:cloud_provider_setup_supported') == True %}
    - repl: CMF_SERVER_ARGS="-i /etc/cloudera-scm-server/cm.settings -cp {{ salt['pillar.get']('platform') }} -env {{ cloud_type }}"
    - unless: grep "CMF_SERVER_ARGS=\"-i /etc/cloudera-scm-server/cm.settings -cp {{ salt['pillar.get']('platform') }}\"" /etc/default/cloudera-scm-server
{% elif salt['pillar.get']('cloudera-manager:settings:set_cdp_env') == True %}
    - repl: CMF_SERVER_ARGS="-i /etc/cloudera-scm-server/cm.settings -env {{ cloud_type }}"
    - unless: grep "CMF_SERVER_ARGS=\"-i /etc/cloudera-scm-server/cm.settings -env {{ cloud_type }}\"" /etc/default/cloudera-scm-server
{% else %}
    - repl: CMF_SERVER_ARGS="-i /etc/cloudera-scm-server/cm.settings"
    - unless: grep "CMF_SERVER_ARGS=\"-i /etc/cloudera-scm-server/cm.settings\"" /etc/default/cloudera-scm-server
{% endif %}

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

{% endif %}

{% if salt['pillar.get']('cloudera-manager:license', None) != None %}

/etc/cloudera-scm-server/license.txt:
  file.managed:
    - source: salt://cloudera/manager/license.txt
    - user: cloudera-scm
    - group: cloudera-scm
    - mode: 600
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

replace_crl_in_cmca_profile:
  file.replace:
    - name: /etc/cloudera-scm-server/cmSubCaCert.profile
    - pattern: "policyset.cmSubCaCertSet.9.default.params.crlDistPointsPointName_0=http://changeme.com/ipa/crl/MasterCRL.bin"
    - repl: "policyset.cmSubCaCertSet.9.default.params.crlDistPointsPointName_0=http://ipa-ca.{{ metadata.cluster_domain }}/ipa/crl/MasterCRL.bin"

replace_ocsp_in_cmca_profile:
  file.replace:
    - name: /etc/cloudera-scm-server/cmSubCaCert.profile
    - pattern: "policyset.cmSubCaCertSet.5.default.params.authInfoAccessADLocation_0=http://changeme.com/ca/ocsp"
    - repl: "policyset.cmSubCaCertSet.5.default.params.authInfoAccessADLocation_0=http://ipa-ca.{{ metadata.cluster_domain }}/ca/ocsp"

/opt/salt/scripts/cm-setup-autotls.sh:
  file.managed:
    - makedirs: True
    - source: salt://cloudera/manager/scripts/setup-autotls.sh.j2
    - template: jinja
    - mode: 700
    - context:
        cm_keytab: {{ cloudera_manager.cm_keytab }}
        server_address: {{ metadata.server_address }}

run_autotls_setup:
  cmd.run:
    - name: /opt/salt/scripts/cm-setup-autotls.sh 2>&1 | tee -a /var/log/cm-setup-autotls.log && exit ${PIPESTATUS[0]}
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

setup_support_settings:
  file.blockreplace:
    - name: /etc/cloudera-scm-server/cm.settings
    - marker_start: "# BLOCK TOP : salt managed zone : please do not edit"
    - marker_end: "# BLOCK BOTTOM : end of salt managed zone --"
    - content: |
        setsettings PHONE_HOME true
        setsettings CLUSTER_STATS_COUNT 2{% if salt['pillar.get']('cloudera-manager:settings:disable_auto_bundle_collection') == True %}
        setsettings CLUSTER_STATS_SCHEDULE NEVER{% endif %}
    - show_changes: True
    - append_if_not_found: True

/opt/salt/scripts/cm_generate_agent_tokens.sh:
  file.managed:
    - makedirs: True
    - source: salt://cloudera/manager/scripts/generate_agent_tokens.sh.j2
    - template: jinja
    - mode: 700

run_generate_agent_tokens:
  cmd.run:
    - name: /opt/salt/scripts/cm_generate_agent_tokens.sh 2>&1 | tee -a /var/log/cm_generate_agent_tokens.log && exit ${PIPESTATUS[0]}
    - require:
        - file: /opt/salt/scripts/cm_generate_agent_tokens.sh
        - cmd: run_autotls_setup
{% endif %}

{% if "ipa_member" in grains.get('roles', []) %}

replace_ipa_env_host_to_server:
  file.replace:
    - name: /opt/cloudera/cm/bin/gen_credentials_ipa.sh
    - pattern: "ipa env host"
    - repl: "ipa env server"

replace_max_renew_life_ipa:
  file.replace:
    - name: /opt/cloudera/cm/bin/gen_credentials_ipa.sh
    - pattern: "MAX_RENEW_LIFE=.*"
    - repl: "MAX_RENEW_LIFE=0"

replace_max_renew_life:
  file.replace:
    - name: /opt/cloudera/cm/bin/gen_credentials.sh
    - pattern: "MAX_RENEW_LIFE=.*"
    - repl: "MAX_RENEW_LIFE=0"

{% endif %}