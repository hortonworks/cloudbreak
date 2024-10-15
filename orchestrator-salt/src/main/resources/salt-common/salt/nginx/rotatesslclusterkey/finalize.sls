finalize-nginx-ssl-cluster-key:
  cmd.run:
    - name: runuser -l root -s /bin/bash -c '/opt/salt/scripts/nginx-ssl-cluster-key-rotation-helper.sh finalize' 2>&1 | tee -a /var/log/finalize-nginx-ssl-cluster-key-rotation.log && exit ${PIPESTATUS[0]}