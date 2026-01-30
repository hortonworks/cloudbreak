get_uuid_list:
  file.managed:
    - name: /opt/salt/scripts/get-uuid-list.sh
    - source: salt://disks/mount/scripts/get-uuid-list.j2
    - template: jinja
    - makedirs: True
    - mode: 755

execute_get_uuid_list:
  cmd.run:
    - name: /opt/salt/scripts/get-uuid-list.sh