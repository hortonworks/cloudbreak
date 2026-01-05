{% if "mount_disks" in grains.get('roles', []) %}

format_and_mount_common:
  file.managed:
    - name: /opt/salt/scripts/format-and-mount-common.sh
    - source: salt://disks/mount/scripts/format-and-mount-common.j2
    - template: jinja
    - makedirs: True
    - mode: 755

format_and_mount_initialize:
  file.managed:
    - name: /opt/salt/scripts/format-and-mount-initialize.sh
    - source: salt://disks/mount/scripts/format-and-mount-initialize.sh
    - makedirs: True
    - mode: 755

find_device_and_format:
  file.managed:
    - name: /opt/salt/scripts/find-device-and-format.sh
    - source: salt://disks/mount/scripts/find-device-and-format.j2
    - template: jinja
    - makedirs: True
    - mode: 755

mount_disks:
  file.managed:
    - name: /opt/salt/scripts/mount-disks.sh
    - source: salt://disks/mount/scripts/mount-disks.j2
    - template: jinja
    - makedirs: True
    - mode: 755

execute_format_and_mount_initialize:
  cmd.run:
    - name: /opt/salt/scripts/format-and-mount-initialize.sh 2>&1 | tee -a /var/log/format-and-mount-initialize-allout.log && exit ${PIPESTATUS[0]}

execute_find_device_and_format:
  cmd.run:
    - name: /opt/salt/scripts/find-device-and-format.sh 2>&1 | tee -a /var/log/find-device-and-format-allout.log && exit ${PIPESTATUS[0]}

execute_mount_disks:
  cmd.run:
    - name: /opt/salt/scripts/mount-disks.sh 2>&1 | tee -a /var/log/mount-disks-allout.log && exit ${PIPESTATUS[0]}

{% endif %}