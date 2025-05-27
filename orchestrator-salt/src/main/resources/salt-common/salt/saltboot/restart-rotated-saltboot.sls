{% if salt['pillar.get']('platform') == 'GCP' %}
change_saltboot_config_location:
  file.append:
    - name: /etc/systemd/system/salt-bootstrap.service
    - text: "Environment='SALTBOOT_CONFIG=/etc/salt-bootstrap/rotated-security-config.yml'"

systemctl_reload_on_saltboot_unit_change:
  cmd.run:
    - name: systemctl --system daemon-reload
    - onchanges:
      - file: change_saltboot_config_location
{% endif %}

stop_salt_bootstrap:
  service.dead:
    - name: salt-bootstrap

start_salt_bootstrap:
  service.running:
    - name: salt-bootstrap