/opt/salt/scripts/freeipa_check_replication_cleanup.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/freeipa_check_replication_cleanup.sh

verify_replication_cleanup:
  cmd.run:
    - name: /opt/salt/scripts/freeipa_check_replication_cleanup.sh 2>&1 | tee -a /var/log/freeipa_replication_cleanup.log && exit ${PIPESTATUS[0]}
    - env:
        - LDAP_URI: "ldap://localhost"
        - FPW: "{{salt['pillar.get']('freeipa:password')}}"
        - TARGET_HOSTS: "{{salt['pillar.get']('freeipa:replication_cleanup:removed_hosts', '')}}"
        - TIMEOUT_SECONDS: "{{salt['pillar.get']('freeipa:replication_cleanup:timeout_sec', 600)}}"
        - POLL_INTERVAL_SECONDS: "{{salt['pillar.get']('freeipa:replication_cleanup:interval_sec', 10)}}"
    - failhard: True
    - require:
        - file: /opt/salt/scripts/freeipa_check_replication_cleanup.sh
