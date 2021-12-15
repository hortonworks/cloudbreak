execute_cdp_logging_agent_doctor:
  cmd.run:
    - name: "sh /opt/salt/scripts/cdp_logging_agent_check.sh doctor"
    - runas: root
    - require:
      - file: /opt/salt/scripts/cdp_logging_agent_check.sh