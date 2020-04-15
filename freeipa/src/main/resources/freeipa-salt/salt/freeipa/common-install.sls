update_cnames:
  cmd.run:
    - name: /opt/salt/scripts/update_cnames.sh
    - env:
        - FPW: {{salt['pillar.get']('freeipa:password')}}
        - DOMAIN: {{salt['pillar.get']('freeipa:domain')}}
        - REALM: {{salt['pillar.get']('freeipa:realm')}}
    - require:
        - file: /opt/salt/scripts/update_cnames.sh

one_week_next_update_grace_period:
  file.replace:
    - name: /var/lib/pki/pki-tomcat/ca/conf/CS.cfg
    - pattern: ^ca[.]crl[.]MasterCRL[.]nextUpdateGracePeriod=.*
    - repl: ca.crl.MasterCRL.nextUpdateGracePeriod=10080
    - unless: grep "^ca[.]crl[.]MasterCRL[.]nextUpdateGracePeriod=10080$" /var/lib/pki/pki-tomcat/ca/conf/CS.cfg

restart_pki-tomcat:
  service.running:
    - name: pki-tomcatd@pki-tomcat
    - onlyif: test -f /etc/ipa/default.conf
    - watch:
      - file: /var/lib/pki/pki-tomcat/ca/conf/CS.cfg

/usr/lib/python2.7/site-packages/ipaserver/plugins/getkeytab.py:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 644
    - source: salt://freeipa/scripts/getkeytab.py
    - onlyif: test -f /etc/ipa/default.conf

restart_freeipa_after_plugin_change:
  service.running:
    - name: ipa
    - onlyif: test -f /etc/ipa/default.conf
    - watch:
      - file: /usr/lib/python2.7/site-packages/ipaserver/plugins/getkeytab.py

set_number_of_krb5kdc_workers:
  file.replace:
    - name: /etc/sysconfig/krb5kdc
    - pattern: ^KRB5KDC_ARGS=.*
    - repl: KRB5KDC_ARGS='-w 100'
    - unless: grep "^KRB5KDC_ARGS='-w 100'$" /etc/sysconfig/krb5kdc

restart_krb5kdc:
  service.running:
    - name: krb5kdc
    - watch:
      - file: /etc/sysconfig/krb5kdc

/etc/httpd/conf.d/ipa-rewrite.conf:
  file.managed:
    - template: jinja
    - user: root
    - group: root
    - mode: 644
    - source: salt://freeipa/templates/ipa-rewrite.conf.j2

restart_httpd:
  service.running:
    - name: httpd
    - watch:
      - file: /etc/httpd/conf.d/ipa-rewrite.conf

install-healthagent:
  cmd.run:
    - name: /opt/salt/scripts/freeipa_healthagent_setup.sh
    - env:
        - FPW: {{salt['pillar.get']('freeipa:password')}}
    - failhard: True
    - unless: /opt/salt/scripts/freeipa_check_replication.sh
    - require:
      - file: /opt/salt/scripts/freeipa_healthagent_setup.sh

firstpass-healthagent:
  cmd.run:
    - name: /cdp/ipahealthagent/freeipa_healthagent_getcerts.sh
    - failhard: True
    - unless: test -f /cdp/ipahealthagent/publicCert.pem
    - require:
      - file: /cdp/ipahealthagent/freeipa_healthagent_getcerts.sh
