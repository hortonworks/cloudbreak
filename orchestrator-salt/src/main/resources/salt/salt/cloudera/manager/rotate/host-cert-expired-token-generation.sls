run_generate_agent_tokens_for_expired_certs:
  cmd.run:
    - name: /opt/salt/scripts/cm_generate_agent_tokens.sh 2>&1 | tee -a /var/log/cm_generate_agent_tokens.log && exit ${PIPESTATUS[0]}
