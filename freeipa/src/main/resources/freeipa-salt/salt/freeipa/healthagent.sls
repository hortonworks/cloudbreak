/opt/salt/scripts/freeipa_healthagent_setup.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/freeipa_healthagent_setup.sh

/cdp/ipahealthagent/freeipa_healthagent_getcerts.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/freeipa_healthagent_getcerts.sh

setup-healthagent:
  cmd.run:
    - name: /opt/salt/scripts/freeipa_healthagent_setup.sh
    - env:
        - FPW: {{salt['pillar.get']('freeipa:password')}}
    - failhard: True
    - require:
      - file: /opt/salt/scripts/freeipa_healthagent_setup.sh

setup-healthagent-certs:
  cmd.run:
    - name: /cdp/ipahealthagent/freeipa_healthagent_getcerts.sh
    - failhard: True
    - unless: test -f /cdp/ipahealthagent/publicCert.pem
    - require:
      - file: /cdp/ipahealthagent/freeipa_healthagent_getcerts.sh

start-freeipa-healthagent:
  service.running:
    - name: cdp-freeipa-healthagent
    - failhard: True
    - enable: True