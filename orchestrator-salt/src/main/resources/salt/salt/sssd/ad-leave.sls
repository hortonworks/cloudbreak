/opt/salt/scripts/remove_dns.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://sssd/template/remove_dns.j2
    - template: jinja

remove-dns:
  cmd.run:
    - name: /opt/salt/scripts/remove_dns.sh && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/remove-dns-executed
    - unless: test -f /var/log/remove-dns-executed
    - require:
      - file: /opt/salt/scripts/remove_dns.sh

leave-domain:
  cmd.run:
    - name: echo $BINDPW | realm leave -v --remove --user={{salt['pillar.get']('sssd-ad:username')}}
    - onlyif: realm list | grep -qi {{salt['pillar.get']('sssd-ad:domain')}}
    - env:
        - BINDPW: {{salt['pillar.get']('sssd-ad:password')}}
