/etc/cdp-luks/bin/rotation/luks_key_rotation_helper.sh:
  file.managed:
    - name: /etc/cdp-luks/bin/rotation/luks_key_rotation_helper.sh
    - user: root
    - group: root
    - mode: 700
    - makedirs: True
    - source: salt://{{ slspath }}/scripts/luks_key_rotation_helper.sh

/etc/cdp-luks/bin/rotation/add_new_key.sh:
  file.managed:
    - name: /etc/cdp-luks/bin/rotation/add_new_key.sh
    - user: root
    - group: root
    - mode: 700
    - makedirs: True
    - source: salt://{{ slspath }}/scripts/add_new_key.sh

Call add_new_key.sh:
  cmd.run:
    - name: /etc/cdp-luks/bin/rotation/add_new_key.sh 2>&1 | tee -a /var/log/cdp-luks/add_new_key-$(date +"%F-%T").log && exit ${PIPESTATUS[0]}
    - runas: root
    - shell: /bin/bash
    - failhard: True
    - require:
      - file: /etc/cdp-luks/bin/rotation/luks_key_rotation_helper.sh
      - file: /etc/cdp-luks/bin/rotation/add_new_key.sh

Change log permissions:
  cmd.run:
    - name: chmod 600 /var/log/cdp-luks/add_new_key-*.log
    - runas: root
    - shell: /bin/bash
