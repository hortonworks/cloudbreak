/opt/salt/scripts/nginx-ssl-cluster-key-rotation-helper.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - user: root
    - group: root
    - source: salt://nginx/rotatesslclusterkey/scripts/nginx-ssl-cluster-key-rotation-helper.sh
    - template: jinja
    - replace: True

prepare-nginx-ssl-cluster-key-rotation:
  cmd.run:
    - name: runuser -l root -s /bin/bash -c '/opt/salt/scripts/nginx-ssl-cluster-key-rotation-helper.sh prepare' 2>&1 | tee -a /var/log/prepare-nginx-ssl-cluster-key-rotation.log && exit ${PIPESTATUS[0]}
    - require:
      - file: /opt/salt/scripts/nginx-ssl-cluster-key-rotation-helper.sh