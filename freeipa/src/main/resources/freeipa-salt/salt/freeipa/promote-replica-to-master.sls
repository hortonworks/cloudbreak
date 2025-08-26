/opt/salt/scripts/freeipa_promote_replica_to_master.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/freeipa_promote_replica_to_master.sh
    - template: jinja

install-freeipa-promote-replica-to-master:
  cmd.run:
    - name: /opt/salt/scripts/freeipa_promote_replica_to_master.sh && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/freeipa_promote_replica_to_master-executed
    - env:
        - FPW: {{salt['pillar.get']('freeipa:password')}}
        - ADMIN_USER: {{salt['pillar.get']('freeipa:admin_user')}}
    - failhard: True
    - unless: test -f /var/log/freeipa_promote_replica_to_master-executed
    - require:
        - file: /opt/salt/scripts/freeipa_promote_replica_to_master.sh
