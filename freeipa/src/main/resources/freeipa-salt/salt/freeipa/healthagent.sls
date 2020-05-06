install-healthagent:
  cmd.run:
    - name: /opt/salt/scripts/freeipa_healthagent_setup.sh
    - env:
        - FPW: {{salt['pillar.get']('freeipa:password')}}
    - failhard: True
    - unless: /opt/salt/scripts/freeipa_check_replication.sh
    - require:
      - file: /opt/salt/scripts/freeipa_healthagent_setup.sh
      - file: /opt/salt/scripts/freeipa_check_replication.sh

firstpass-healthagent:
  cmd.run:
    - name: /cdp/ipahealthagent/freeipa_healthagent_getcerts.sh
    - failhard: True
    - unless: test -f /cdp/ipahealthagent/publicCert.pem
    - require:
      - file: /cdp/ipahealthagent/freeipa_healthagent_getcerts.sh
