execute_cdp_logging_agent_doctor:
  cmd.run:
    - name: "sh /opt/salt/scripts/cdp_logging_agent_check.sh doctor"
    - onlyif: "test -f /opt/salt/scripts/cdp_logging_agent_check.sh"