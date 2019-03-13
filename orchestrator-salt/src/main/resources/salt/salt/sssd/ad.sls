packages_install:
  pkg.installed:
    - refresh: False
    - pkgs:
{% if grains['os_family'] == 'Debian' %}
      - sssd-ad
      - realmd
      - sssd-tools
      - sssd
      - libnss-sss
      - libpam-sss
      - adcli
      - packagekit
      - policykit-1
      - samba-common-bin
      - samba-libs
      - samba-dsdb-modules
      - krb5-user
{% elif grains['os_family'] == 'Suse' %}
      - krb5-client
      - samba-client
      - sssd
      - sssd-ad
{% else %}
      - sssd
      - realmd
      - krb5-workstation
      - samba-common-tools
      - openldap-clients
{% endif %}

join_domain:
  cmd.run:
{% if grains['os_family'] == 'Debian' %}
    - name: echo $BINDPW | realm --verbose join --install=/ --user={{salt['pillar.get']('sssd-ad:username')}} {{salt['pillar.get']('sssd-ad:domain')}}
{% else %}
    - name: echo $BINDPW | realm --verbose join --user={{salt['pillar.get']('sssd-ad:username')}} {{salt['pillar.get']('sssd-ad:domain')}}
{% endif %}
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
    - mode: 700

include:
    - sssd.ssh

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