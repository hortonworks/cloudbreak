{% if "unmount_block_storages" in grains.get('roles', []) %}

unmount_block_storages:
  file.managed:
    - name: /opt/salt/scripts/unmount-block-storages.sh
    - source: salt://disks/mount/scripts/unmount-block-storages.j2
    - template: jinja
    - makedirs: True
    - mode: 755

execute_unmount_block_storages_initialize:
  cmd.run:
    - name: /opt/salt/scripts/unmount-block-storages.sh 2>&1 | tee -a /var/log/unmount-block-storages-allout.log && exit ${PIPESTATUS[0]}

{% endif %}