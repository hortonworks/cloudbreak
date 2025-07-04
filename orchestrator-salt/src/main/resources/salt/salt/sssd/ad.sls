{%- from 'sssd/ad-settings.sls' import ad with context %}

ad_packages_install:
  pkg.installed:
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
    - name: realm leave; echo $BINDPW | realm --verbose join --computer-name={{ ad.server_hostname }} --user={{salt['pillar.get']('sssd-ad:username')}} {{salt['pillar.get']('sssd-ad:domain')}} 2>&1 | tee -a /var/log/realm.log && exit ${PIPESTATUS[1]}
{% endif %}
    - unless: realm list | grep -qi {{salt['pillar.get']('sssd-ad:domain')}} && test -f /etc/krb5.keytab
    - failhard: True
    - env:
      - BINDPW: {{salt['pillar.get']('sssd-ad:password')}}

restart-sssd-if-reconfigured:
  service.running:
    - enable: True
    - name: sssd
    - failhard: True
    - watch:
      - file: /etc/sssd/sssd.conf

/etc/sssd/sssd.conf:
  file.managed:
    - source: salt://sssd/template/sssd-ad.j2
    - template: jinja
    - mode: 700

include:
    - sssd.ssh

/opt/salt/scripts/add_ad_dns.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://sssd/template/add_ad_dns.j2
    - template: jinja

add-ad-dns:
  cmd.run:
    - name: /opt/salt/scripts/add_ad_dns.sh && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/add-ad-dns-executed
    - unless: test -f /var/log/add-ad-dns-executed
    - failhard: True
    - require:
        - file: /opt/salt/scripts/add_ad_dns.sh

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
    - failhard: True
    - require:
      - file: /opt/salt/scripts/reverse_dns.sh

dns_resolution_fix_domain:
  file.append:
    - name: /etc/resolv.conf
    - text: "domain {{ salt['grains.get']('domain') }}"

dns_resolution_fix_search:
  file.replace:
    - name: /etc/resolv.conf
    - pattern: "^search.*"
    - repl: "search {{ salt['grains.get']('domain') }}"

{%- if "manager_server" in grains.get('roles', []) %}

ldif_to_add_machine_to_domain_admin:
  file.managed:
    - name: /opt/salt/add-machine-to-admin.ldif
    - contents: |
        dn: CN=Domain Admins,CN=Users,{{ ad.domain_component }}
        changetype: modify
        add: member
        member: CN={{ ad.server_hostname }},CN=Computers,{{ ad.domain_component }}

ldapmodify_make_machine_admin:
  cmd.run:
    - name: ldapmodify -H {{ salt['pillar.get']('ldap:connectionURL') }} -D "{{ salt['pillar.get']('ldap:bindDn') }}" -w {{ salt['pillar.get']('ldap:bindPasswordEscaped') }} -f /opt/salt/add-machine-to-admin.ldif
    - failhard: True
    - unless: ldapsearch -LLL -H {{ salt['pillar.get']('ldap:connectionURL') }} -D "{{ salt['pillar.get']('ldap:bindDn') }}" -w {{ salt['pillar.get']('ldap:bindPasswordEscaped') }} -b "CN=Domain Admins,CN=Users,{{ ad.domain_component }}" member | grep -q "CN={{ ad.server_hostname }},CN=Computers,{{ ad.domain_component }}"

{%- endif %}

