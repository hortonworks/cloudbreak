admin:
  user.present:
    - shell: /bin/bash
    - home: /home/admin
    - groups:
      - wheel
      - users

/home/admin/.ssh:
  file.directory:
    - user: admin
    - group: admin
    - makedirs: True

/home/admin/.ssh/authorized_keys:
  file.copy:
    - source: /home/cloudbreak/.ssh/authorized_keys
    - user: admin
    - unless: ls -1 /home/admin/.ssh/authorized_keys