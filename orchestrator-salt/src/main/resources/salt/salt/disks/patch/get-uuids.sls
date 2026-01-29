get-uuids:
  cmd.run:
    - name: blkid -s UUID {{ (salt['pillar.get']('disk_patch')[salt['grains.get']('fqdn')])['attached_devices'] | default("") }}
    - unless: '[ -z "{{ (salt['pillar.get']('disk_patch')[salt['grains.get']('fqdn')])['attached_devices'] | default("") }}" ]'
