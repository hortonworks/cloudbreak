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


