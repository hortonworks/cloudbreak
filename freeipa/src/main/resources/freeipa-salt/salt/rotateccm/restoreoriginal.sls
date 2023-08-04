restore_last_ccm_jumpgate_conf:
  file.copy:
    - name: /etc/jumpgate/config.toml
    - source: /etc/jumpgate/config.toml.backup
    - user: jumpgate
    - group: jumpgate
    - force: True

delete_config_backup:
  file.absent:
    - name: /etc/jumpgate/config.toml.backup
    - onchanges:
      - file: restore_last_ccm_jumpgate_conf

restart_jumpgate_agent_after_conf_restore:
  cmd.run:
    - name: systemctl restart jumpgate-agent
    - runas: root
    - onchanges:
      - file: restore_last_ccm_jumpgate_conf
