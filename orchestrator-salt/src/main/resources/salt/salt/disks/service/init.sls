mount_instance_storage_script:
  file.managed:
    - name: /opt/salt/scripts/mount-instance-storage.sh
    - source: salt://disks/service/scripts/mount-instance-storage.j2
    - template: jinja
    - makedirs: True
    - mode: 755

mount_instance_storage_service_file:
  file.managed:
    - user: root
    - group: root
    - name: /etc/systemd/system/mount-instance-storage.service
    - makedirs: True
    - source: salt://disks/service/scripts/mount-instance-storage.service

mount_instance_storage_service_start:
  service.running:
    - name: mount-instance-storage
    - enable: True
    - reload: True
    - require:
      - file: mount_instance_storage_service_file