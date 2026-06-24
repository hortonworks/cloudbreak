/opt/salt/scripts/check_cert_expiration.sh:
  file.managed:
    - makedirs: True
    - source: salt://cloudera/manager/rotate/scripts/check_cert_expiration.sh.j2
    - template: jinja
    - mode: 700

check-cm-agent-cert-expiration:
  cmd.run:
    - name: /opt/salt/scripts/check_cert_expiration.sh 1> >(tee -a /var/log/cm_agent_check_cert_expiration.log >&1) 2> >(tee -a /var/log/cm_agent_check_cert_expiration.log >&2)
    - require:
      - file: /opt/salt/scripts/check_cert_expiration.sh
