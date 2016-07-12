admin:
  user.present:
    - shell: /bin/bash
    - home: /home/admin
    - groups:
      - wheel
      - users
    - optional_groups:
      - hadoop
      - hdfs

/home/admin/.ssh:
  file.directory:
    - user: admin
    - group: admin
    - makedirs: True

/home/admin/.ssh/authorized_keys:
  file.copy:
    - source: /home/cloudbreak/.ssh/authorized_keys
    - user: admin