/opt/salt/scripts/freeipa_verify_replica_health.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/freeipa_verify_replica_health.sh

# Replication convergence gate, run on the promoted primary gateway after a primary gateway change and on every surviving
# master after a downscale (once the removed hosts are cleaned up). The master's agreements must be healthy on both
# suffixes and in both directions before the flow chain removes a master or seeds the next replica from this one.
# Hosts just removed from the topology (exclude_hosts) are ignored. Observe-only, fails fast on timeout.
verify_replication_convergence:
  cmd.run:
    - name: /opt/salt/scripts/freeipa_verify_replica_health.sh 2>&1 | tee -a /var/log/freeipa_replication_convergence.log && exit ${PIPESTATUS[0]}
    - env:
        - FPW: "{{salt['pillar.get']('freeipa:password')}}"
        - DOMAIN: "{{salt['pillar.get']('freeipa:domain')}}"
        - REALM: "{{salt['pillar.get']('freeipa:realm')}}"
        - ADMIN_USER: "{{salt['pillar.get']('freeipa:admin_user')}}"
        - EXCLUDE_HOSTS: "{{salt['pillar.get']('freeipa:replication_convergence:exclude_hosts', '')}}"
        - TIMEOUT_SECONDS: "{{salt['pillar.get']('freeipa:replication_convergence:timeout_sec', 300)}}"
        - POLL_INTERVAL_SECONDS: "{{salt['pillar.get']('freeipa:replication_convergence:interval_sec', 10)}}"
    - failhard: True
    - require:
        - file: /opt/salt/scripts/freeipa_verify_replica_health.sh
