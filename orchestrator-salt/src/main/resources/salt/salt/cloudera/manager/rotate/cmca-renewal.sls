/opt/salt/scripts/cmca_renewal.sh:
  file.managed:
    - makedirs: True
    - mode: 700
    - source: salt://cloudera/manager/scripts/cmca_renewal.sh
    - template: jinja
    - replace: True

renew-cmca:
  cmd.run:
    - name: /opt/salt/scripts/cmca_renewal.sh 2>&1 | tee -a /var/log/cm_cmca_renewal.log && exit ${PIPESTATUS[0]}
    - require:
      - file: /opt/salt/scripts/cmca_renewal.sh
    - env:
       - AUTO_TLS_KEYSTORE_PASSWORD: {{salt['pillar.get']('cloudera-manager:autotls:keystore_password')}}
       - AUTO_TLS_TRUSTSTORE_PASSWORD: {{salt['pillar.get']('cloudera-manager:autotls:truststore_password')}}

{% if salt['pillar.get']('cluster:gov_cloud', False) == True %}

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
      - cmd: renew-cmca

{% endif %}