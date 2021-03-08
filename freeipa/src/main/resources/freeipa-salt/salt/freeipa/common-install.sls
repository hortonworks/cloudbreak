update_cnames:
  cmd.run:
    - name: /opt/salt/scripts/update_cnames.sh
    - env:
        - FPW: {{salt['pillar.get']('freeipa:password')}}
        - DOMAIN: {{salt['pillar.get']('freeipa:domain')}}
        - REALM: {{salt['pillar.get']('freeipa:realm')}}
        - ADMIN_USER: {{salt['pillar.get']('freeipa:admin_user')}}
    - require:
        - file: /opt/salt/scripts/update_cnames.sh

one_week_next_update_grace_period:
  cmd.run:
    - names:
      - service pki-tomcatd@pki-tomcat stop
      - sed -i 's/^ca[.]crl[.]MasterCRL[.]nextUpdateGracePeriod=.*/ca.crl.MasterCRL.nextUpdateGracePeriod=10080/' /var/lib/pki/pki-tomcat/ca/conf/CS.cfg
      - service pki-tomcatd@pki-tomcat start
    - unless: grep "^ca[.]crl[.]MasterCRL[.]nextUpdateGracePeriod=10080$" /var/lib/pki/pki-tomcat/ca/conf/CS.cfg

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

/etc/httpd/conf/httpd-log-filter.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/httpd-log-filter.sh

configure_httpd_log_filter:
  file.replace:
    - name: /etc/httpd/conf/httpd.conf
    - pattern: ^ErrorLog.*
    - repl: ErrorLog "|/etc/httpd/conf/httpd-log-filter.sh"
    - unless: grep "httpd-log-filter.sh" /etc/httpd/conf/httpd.conf

disable_http_trace:
  file.append:
    - name: /etc/httpd/conf/httpd.conf
    - text: 'TraceEnable Off'

restart_httpd:
  service.running:
    - name: httpd
    - watch:
      - file: /etc/httpd/conf.d/ipa-rewrite.conf
      - file: /etc/httpd/conf/httpd.conf
