{%- if "ID_BROKER_CLOUD_IDENTITY_ROLE" in grains.get('roles', []) %}

disable-sshd:
  service.dead:
    - enable: False
    - name: sshd

{% else %}

{% if not salt['file.directory_exists']('/yarn-private') %}

restart-sshd-if-reconfigured:
  service.running:
    - enable: True
    - name: sshd
    - watch:
      - file: /etc/ssh/sshd_config

{% endif %}

enable_password_ssh_auth:
  file.replace:
    - name: /etc/ssh/sshd_config
    - append_if_not_found: True
    - pattern: "PasswordAuthentication no"
    - repl: "PasswordAuthentication yes"

{% endif %}
