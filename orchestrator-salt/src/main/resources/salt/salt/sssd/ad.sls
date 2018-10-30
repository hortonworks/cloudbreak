packages_install:
  pkg.installed:
    - refresh: False
    - pkgs:
      - sssd
      - realmd
      - krb5-workstation
      - samba-common-tools

join_domain:
  cmd.run:
    - name: echo $BINDPW | realm --verbose join --user={{salt['pillar.get']('sssd-ad:username')}} {{salt['pillar.get']('sssd-ad:domain')}}
    - unless: realm list | grep -qi {{salt['pillar.get']('sssd-ad:domain')}}
    - env:
      - BINDPW: {{salt['pillar.get']('sssd-ad:password')}}

restart-sssd-if-reconfigured:
  service.running:
    - enable: True
    - name: sssd
    - watch:
      - file: /etc/sssd/sssd.conf

/etc/sssd/sssd.conf:
  file.managed:
    - source: salt://sssd/template/sssd-ad.j2
    - template: jinja

restart-sshd-if-reconfigured:
  service.running:
    - enable: True
    - name: sshd
    - watch:
      - file: /etc/ssh/sshd_config

enable_password_ssh_auth:
  file.replace:
    - name: /etc/ssh/sshd_config
    - append_if_not_found: True
    - pattern: "PasswordAuthentication no"
    - repl: "PasswordAuthentication yes"

/opt/salt/scripts/reverse_dns.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://sssd/template/reverse_dns.j2
    - template: jinja

add-dns-ptr:
  cmd.run:
    - name: /opt/salt/scripts/reverse_dns.sh && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/add-dns-ptr-executed
    - unless: test -f /var/log/add-dns-ptr-executed
    - require:
      - file: /opt/salt/scripts/reverse_dns.sh