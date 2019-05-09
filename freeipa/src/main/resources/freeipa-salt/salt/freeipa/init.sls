freeipa-install:
  pkg.installed:
    - pkgs:
        - ntp
        - ipa-server
        - ipa-server-dns

net.ipv6.conf.lo.disable_ipv6:
  sysctl.present:
    - value: 0

/opt/salt/scripts/freeipa_install.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/freeipa_install.sh

install-freeipa:
  cmd.run:
    - name: /opt/salt/scripts/freeipa_install.sh && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/freeipa_install-executed
    - env:
        - FPW: {{salt['pillar.get']('freeipa:password')}}
        - DOMAIN: {{salt['pillar.get']('freeipa:domain')}}
        - REALM: {{salt['pillar.get']('freeipa:realm')}}
    - unless: test -f /var/log/freeipa_install-executed
    - require:
        - file: /opt/salt/scripts/freeipa_install.sh


/opt/salt/scripts/ipa_user_management.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/ipa_user_management.sh

add-user:
  cmd.run:
    - name: /opt/salt/scripts/ipa_user_management.sh && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/ipa_user_management-executed
    - env:
        - ADMINUSER: {{salt['pillar.get']('freeipa:password')}}
        - PASSWORD: {{salt['pillar.get']('freeipa:domain')}}
        - USERNAME: {{salt['pillar.get']('freeipa:username')}}
        - FNAME: {{salt['pillar.get']('freeipa:fname')}}
        - LNAME: {{salt['pillar.get']('freeipa:lname')}}
    - unless: test -f /var/log/ipa_user_management-executed
    - require:
        - file: /opt/salt/scripts/ipa_user_management.sh


