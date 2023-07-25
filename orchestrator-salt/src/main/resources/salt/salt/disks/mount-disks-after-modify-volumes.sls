{% if "mount_disks_after_modify_volumes" in grains.get('roles', []) %}

modify_volumes_mount_disks:
  file.managed:
    - name: /opt/salt/scripts/mount-disks-after-modify-volumes.sh
    - source: salt://disks/mount/scripts/mount-disks-after-modify-volumes.j2
    - template: jinja
    - makedirs: True
    - mode: 755

execute_modify_volumes_mount_disks_initialize:
  cmd.run:
    - name: /opt/salt/scripts/mount-disks-after-modify-volumes.sh 2>&1 | tee -a /var/log/mount-disks-after-modify-volumes-allout.log && exit ${PIPESTATUS[0]}

{% endif %}