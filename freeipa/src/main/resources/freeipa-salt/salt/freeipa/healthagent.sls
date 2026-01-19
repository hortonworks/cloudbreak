{% if salt['file.directory_exists']('/cdp/ipahealthagent') %}

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

/cdp/ipahealthagent/httpd-crt-tracking.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/httpd-crt-tracking.sh
    - onlyif: test -f /var/lib/ipa/certs/httpd.crt

/lib/systemd/system/httpd-crt-change-tracker.service:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - source: salt://freeipa/services/httpd-crt-change-tracker.service
    - onlyif: test -f /var/lib/ipa/certs/httpd.crt
    - require:
      - file: /cdp/ipahealthagent/httpd-crt-tracking.sh

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

/cdp/ipahealthagent/config.yaml:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 600
    - source: salt://freeipa/templates/ipahealthagent_config.yaml.j2
    - template: jinja

# For backward compatibility
/opt/ipahealthagent/config.yaml:
  file.symlink:
    - makedirs: True
    - user: root
    - group: root
    - mode: 600
    - target: /cdp/ipahealthagent/config.yaml
    - force: True
    - require:
        - file: /cdp/ipahealthagent/config.yaml

start-freeipa-healthagent:
  service.running:
    - name: cdp-freeipa-healthagent
    - failhard: True
    - enable: True
    - watch:
        - file: /cdp/ipahealthagent/config.yaml

{% endif %}