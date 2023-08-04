finalize_rotate:
  schedule.disabled:
    - name: restore_rotate_job

  file.absent:
    - name: /etc/jumpgate/config.toml.backup
