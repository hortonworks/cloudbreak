{%- set local_backup_dir = salt['pillar.get']('backup_restore_config:temp_backup_dir', '/var/tmp') %}

get_free_space:
  cmd.run:
    - name: /opt/salt/scripts/get_free_space.sh "{{ local_backup_dir }}"
