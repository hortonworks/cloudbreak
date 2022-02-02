# run logging agent doctor at every 4 hour
0 */4 * * * root sh /opt/salt/scripts/cdp_logging_agent_check.sh doctor
