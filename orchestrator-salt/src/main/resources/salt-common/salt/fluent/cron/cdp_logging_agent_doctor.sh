# run logging agent doctor every day at 01:00:00
0 1 * * * root sh /opt/salt/scripts/cdp_logging_agent_check.sh doctor
