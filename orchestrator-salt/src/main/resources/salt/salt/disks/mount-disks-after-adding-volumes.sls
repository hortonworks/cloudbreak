{% if "mount_disks_after_adding_volumes" in grains.get('roles', []) %}

adding_volumes_mount_disks:
  file.managed:
    - name: /opt/salt/scripts/mount-disks-after-adding-volumes.sh
    - source: salt://disks/mount/scripts/mount-disks-after-adding-volumes.j2
    - template: jinja
    - makedirs: True
    - mode: 755

execute_adding_volumes_mount_disks_initialize:
  cmd.run:
    - name: /opt/salt/scripts/mount-disks-after-adding-volumes.sh 2>&1 | tee -a /var/log/mount-disks-after-adding-volumes-allout.log && exit ${PIPESTATUS[0]}

{% endif %}