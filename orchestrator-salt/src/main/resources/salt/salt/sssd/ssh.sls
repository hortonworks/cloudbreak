{%- from 'metadata/settings.sls' import metadata with context %}

{%- if "ID_BROKER_CLOUD_IDENTITY_ROLE" not in grains.get('roles', []) %}
{% if metadata.platform != 'YARN' %}
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