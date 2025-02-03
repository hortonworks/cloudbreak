{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}

{% if cloudera_manager.communication.autotls_enabled == True %}
check_token:
  file.exists:
    - name: /etc/cloudera-scm-agent/cmagent.token
    - retry:
        attempts: 10

/etc/cloudera-scm-agent/cmagent.token:
  file.managed:
    - user: root
    - group: root
    - mode: 600
    - replace: False
    - require:
      - file: check_token
{% endif %}

start_agent:
  service.running:
    - enable: True
    - name: cloudera-scm-agent
{% if cloudera_manager.communication.autotls_enabled == True %}
    - require:
      - file: check_token
{% endif %}



{% if "ad_member" in grains.get('roles', []) and cloudera_manager.communication.autotls_enabled == True %}

copy_cm_ca:
  file.copy:
    - name: /etc/pki/ca-trust/source/anchors/cm-auto-global_cacerts.pem
    - source: /var/lib/cloudera-scm-agent/agent-cert/cm-auto-global_cacerts.pem
    - failhard: True
    - retry:
        attempts: 30
        interval: 30
    - require:
        - service: start_agent
    - unless: test -f /var/lib/cloudera-scm-agent/agent-cert/cm-auto-in_cluster_ca_cert.pem && keytool -list -keystore /etc/pki/ca-trust/extracted/java/cacerts -storepass changeit | grep -q "$(openssl x509 -in /var/lib/cloudera-scm-agent/agent-cert/cm-auto-in_cluster_ca_cert.pem -noout -fingerprint -sha256 | cut -d'=' -f2)"

update_ca_trust_with_cm_ca:
  cmd.run:
    - name: update-ca-trust
    - onchanges:
        - file: copy_cm_ca

{% endif %}