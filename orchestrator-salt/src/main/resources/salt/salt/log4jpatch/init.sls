{% if salt['pillar.get']('log4j_patch_enabled',False) == True %}
/opt/log4jpatch/scripts/cm_cdp_cdh_log4j_jndi_removal.sh:
  file.managed:
    - makedirs: True
    - source: salt://log4jpatch/scripts/cm_cdp_cdh_log4j_jndi_removal.sh
    - mode: 700

/opt/log4jpatch/scripts/run_log4j_patcher.sh:
  file.managed:
    - makedirs: True
    - source: salt://log4jpatch/scripts/run_log4j_patcher.sh
    - mode: 700

run_log4j_patch:
  cmd.run:
    - name: /opt/log4jpatch/scripts/run_log4j_patcher.sh cdp 2>&1 | tee -a /var/log/log4jpatch.log && exit ${PIPESTATUS[0]}
    - runas: root
    - failhard: True
    - env:
      - SKIP_HDFS: "skip"
    - require:
      - file: /opt/log4jpatch/scripts/cm_cdp_cdh_log4j_jndi_removal.sh
      - file: /opt/log4jpatch/scripts/run_log4j_patcher.sh

remove_misplaced_log4jpatch_log_files:
  cmd.run:
    - name: rm -f /**/output_run_log4j_patcher*
    - runas: root
    - require:
      - cmd: run_log4j_patch
{% endif %}