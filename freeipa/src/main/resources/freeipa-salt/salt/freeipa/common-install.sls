update_cnames:
  cmd.run:
    - name: /opt/salt/scripts/update_cnames.sh
    - env:
        - FPW: {{salt['pillar.get']('freeipa:password')}}
        - DOMAIN: {{salt['pillar.get']('freeipa:domain')}}
        - REALM: {{salt['pillar.get']('freeipa:realm')}}
        - ADMIN_USER: {{salt['pillar.get']('freeipa:admin_user')}}
    - failhard: True
    - require:
        - file: /opt/salt/scripts/update_cnames.sh

one_week_next_update_grace_period:
  cmd.run:
    - names:
      - service pki-tomcatd@pki-tomcat stop
      - sed -i 's/^ca[.]crl[.]MasterCRL[.]nextUpdateGracePeriod=.*/ca.crl.MasterCRL.nextUpdateGracePeriod=10080/' /var/lib/pki/pki-tomcat/ca/conf/CS.cfg
      - service pki-tomcatd@pki-tomcat start
    - failhard: True
    - unless: grep "^ca[.]crl[.]MasterCRL[.]nextUpdateGracePeriod=10080$" /var/lib/pki/pki-tomcat/ca/conf/CS.cfg

add-httpd-x-cdp-trace-id:
   file.line:
     - name: /usr/lib/python2.7/site-packages/ipaserver/rpcserver.py
     - mode: ensure
     - content: "        logger.info('X-cdp-request-ID : %s', environ.get('HTTP_X_CDP_REQUEST_ID'))"
     - after: "return self.marshal(result, RefererError(referer=environ['HTTP_REFERER']), _id)"
     - indent: False
     - backup: False

/usr/lib/python2.7/site-packages/ipaserver/plugins/getkeytab.py:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 644
    - source: salt://freeipa/scripts/getkeytab.py
    - onlyif: test -f /etc/ipa/default.conf

/usr/lib/python2.7/site-packages/ipaserver/plugins/stageuser.py:
  file.patch:
    - source: salt://freeipa/scripts/stageuser.patch
    - hash: md5=8a488621d3d8c16677133f1f32810325
    - onlyif: test -f /etc/ipa/default.conf

restart_freeipa_after_plugin_change:
  service.running:
    - name: ipa
    - onlyif: test -f /etc/ipa/default.conf
    - failhard: True
    - watch:
      - file: /usr/lib/python2.7/site-packages/ipaserver/plugins/getkeytab.py
      - file: /usr/lib/python2.7/site-packages/ipaserver/rpcserver.py
      - file: /usr/lib/python2.7/site-packages/ipaserver/plugins/stageuser.py

set_number_of_krb5kdc_workers:
  file.replace:
    - name: /etc/sysconfig/krb5kdc
    - pattern: ^KRB5KDC_ARGS=.*
    - repl: KRB5KDC_ARGS='-w 100'
    - unless: grep "^KRB5KDC_ARGS='-w 100'$" /etc/sysconfig/krb5kdc

restart_krb5kdc:
  service.running:
    - name: krb5kdc
    - failhard: True
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
    - failhard: True
    - watch:
      - file: /etc/httpd/conf.d/ipa-rewrite.conf
      - file: /etc/httpd/conf/httpd.conf

/etc/sssd/sssd.conf:
  file.line:
    - mode: ensure
    - content: "entry_cache_timeout = 30"
    - after: "cache_credentials.*"

restart_sssd_if_reconfigured:
  service.running:
    - enable: True
    - name: sssd
    - failhard: True
    - watch:
      - file: /etc/sssd/sssd.conf

/opt/salt/scripts/disable_anon_ldap.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/disable_anon_ldap.sh

disable-anonymous-ldap-access:
  cmd.run:
    - name: /opt/salt/scripts/disable_anon_ldap.sh && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/freeipa_ldap_anon-executed
    - env:
        - FPW: {{salt['pillar.get']('freeipa:password')}}
    - failhard: True
    - require:
        - file: /opt/salt/scripts/disable_anon_ldap.sh
    - unless: test -f /var/log/freeipa_ldap_anon-executed
