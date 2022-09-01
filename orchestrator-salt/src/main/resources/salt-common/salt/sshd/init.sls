restart_sshd:
  service.running:
    - enable: True
    - name: sshd
    - watch:
      - file: /etc/ssh/sshd_config

fix_sshd_client_timeout:
  file.replace:
    - name: /etc/ssh/sshd_config
    - pattern: "^ClientAliveInterval.*"
    - repl: "ClientAliveInterval 1800"
    - append_if_not_found: True
