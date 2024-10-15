rotate-nginx-ssl-cluster-key:
  cmd.run:
    - name: runuser -l root -s /bin/bash -c '/opt/salt/scripts/nginx-ssl-cluster-key-rotation-helper.sh rotate' 2>&1 | tee -a /var/log/rotate-nginx-ssl-cluster-key.log && exit ${PIPESTATUS[0]}