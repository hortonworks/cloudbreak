/opt/salt/scripts/cdp_logging_agent_check.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://fluent/scripts/cdp_logging_agent_check.sh
    - template: jinja

/etc/cron.d/cdp_logging_agent_doctor:
  file.managed:
    - user: root
    - group: root
    - mode: 600
    - source: salt://fluent/cron/cdp_logging_agent_doctor.sh
    - template: jinja