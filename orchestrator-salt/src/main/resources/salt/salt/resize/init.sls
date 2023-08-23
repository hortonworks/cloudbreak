resize_volumes_script:
  file.managed:
    - name: /opt/salt/scripts/resize-storage-volumes.sh
    - source: salt://resize/scripts/resize-storage-volumes.j2
    - template: jinja
    - makedirs: True
    - mode: 755

{% if "resize_disks" in grains.get('roles', []) %}

execute_resize_volumes_initialize:
  cmd.run:
    - name: /opt/salt/scripts/resize-storage-volumes.sh 2>&1 | tee -a /var/log/resize-storage-volumes-allout.log && exit ${PIPESTATUS[0]}

{% endif %}