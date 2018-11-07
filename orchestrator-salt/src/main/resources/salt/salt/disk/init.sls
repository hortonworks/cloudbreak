/usr/bin/mount-disks.sh:
  file.managed:
    - makedirs: True
    - source: salt://disk/mount-disks.sh
    - mode: 744

mount_disks:
  cmd.run:
    - name: /usr/bin/mount-disks.sh
    - env:
      - CLOUD_PLATFORM: {{ salt['pillar.get']('disk:cloudPlatform') }}
      - START_LABEL: {{ salt['pillar.get']('disk:platformDiskStartLabel') }}
      - PLATFORM_DISK_PREFIX: {{ salt['pillar.get']('disk:platformDiskPrefix') }}