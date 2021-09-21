{% if pillar['keytab']['CM'] is defined and salt['pillar.get']('keytab:CM') != None %}

set_cm_keytab_permission:
  file.managed:
    - replace: False
    - name: {{salt['pillar.get']('keytab:CM:path')}}
    - user: cloudera-scm
    - group: cloudera-scm
    - mode: 600

configure_cm_principal:
  file.managed:
    - name: /etc/cloudera-scm-server/cmf.principal
    - user: cloudera-scm
    - group: cloudera-scm
    - contents_pillar: keytab:CM:principal

replace_ipa_env_host_to_server:
  file.replace:
    - name: /opt/cloudera/cm/bin/gen_credentials_ipa.sh
    - pattern: "IPA_HOST=.*"
    - repl: "IPA_HOST=$(ipa server-find  --pkey-only --raw | grep 'cn' | sed 's/  cn: //g' | head -1)"

add_host_to_getkeytab:
  file.replace:
    - name: /opt/cloudera/cm/bin/gen_credentials_ipa.sh
    - pattern: "keytab=\\$KEYTAB_PATH$"
    - repl: "keytab=$KEYTAB_PATH -s $IPA_HOST"

replace_max_renew_life_ipa:
  file.replace:
    - name: /opt/cloudera/cm/bin/gen_credentials_ipa.sh
    - pattern: "MAX_RENEW_LIFE=.*"
    - repl: "MAX_RENEW_LIFE=0"
    - unless: grep -q "MAX_RENEW_LIFE=0" /opt/cloudera/cm/bin/gen_credentials_ipa.sh

replace_max_renew_life:
  file.replace:
    - name: /opt/cloudera/cm/bin/gen_credentials.sh
    - pattern: "MAX_RENEW_LIFE=.*"
    - repl: "MAX_RENEW_LIFE=0"
    - unless: grep -q "MAX_RENEW_LIFE=0" /opt/cloudera/cm/bin/gen_credentials.sh

{% endif %}