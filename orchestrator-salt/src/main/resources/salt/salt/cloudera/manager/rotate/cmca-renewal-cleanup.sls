/opt/salt/scripts/cmca_renewal_cleanup.sh:
  file.managed:
    - makedirs: True
    - mode: 700
    - source: salt://cloudera/manager/scripts/cmca_renewal_cleanup.sh
    - template: jinja
    - replace: True

renew-cmca:
  cmd.run:
    - name: /opt/salt/scripts/cmca_renewal_cleanup.sh 2>&1 | tee -a /var/log/cm_cmca_renewal_cleanup.log && exit ${PIPESTATUS[0]}
    - require:
      - file: /opt/salt/scripts/cmca_renewal_cleanup.sh