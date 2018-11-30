{%- from 'ambari/settings.sls' import ambari with context %}

{% if ambari.is_systemd %}

stop-ambari-agent:
  module.wait:
    - name: service.systemctl_reload
    - watch:
      - file: /etc/systemd/system/ambari-agent.service
  service.dead:
    - enable: False
    - name: ambari-agent
    - watch:
      - file: /etc/systemd/system/ambari-agent.service

{% else %}

# Upstart case

# Avoid concurrency between SysV and Upstart
disable-ambari-agent-sysv:
  cmd.run:
    - name: chkconfig ambari-agent off
    - onlyif: chkconfig --list ambari-agent | grep on

/etc/init/ambari-agent.override:
  file.managed:
    - source: salt://ambari/upstart/ambari-agent.override

stop-ambari-agent:
  service.dead:
    - enable: False
    - name: ambari-agent

{% endif %}