restart-sshd-if-reconfigured:
  service.running:
    - enable: True
    - name: sshd
    - watch:
      - file: /etc/ssh/sshd_config

enable_password_ssh_auth:
  file.replace:
    - name: /etc/ssh/sshd_config
    - append_if_not_found: True
    - pattern: "PasswordAuthentication no"
    - repl: "PasswordAuthentication yes"