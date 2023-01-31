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

{%- set ipaserverPath = salt.cmd.run('find /usr/lib -name ipaserver') %}

add-httpd-x-cdp-trace-id:
   file.line:
     - name: {{ ipaserverPath }}/rpcserver.py
     - mode: ensure
     - content: "        logger.info('X-cdp-request-ID : %s', environ.get('HTTP_X_CDP_REQUEST_ID'))"
     - after: "return self.marshal(result, RefererError(referer=environ['HTTP_REFERER']), _id)"
     - indent: False
     - backup: False

{{ ipaserverPath }}/plugins/getkeytab.py:
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
    - failhard: True
    - watch:
      - file: {{ ipaserverPath }}/plugins/getkeytab.py
      - file: {{ ipaserverPath }}/rpcserver.py

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

{% if grains['os_family'] == 'RedHat' and grains['osmajorrelease'] | int == 8 %}
  
/etc/named/ipa-ext.conf:
  file.managed:
    - source: salt://freeipa/templates/ipa-ext.conf

/etc/named/ipa-options-ext.conf:
  file.append:
    - text:
        - allow-recursion { trusted_network; };
        - allow-query-cache  { trusted_network; };
    - require:
        - file: /etc/named/ipa-ext.conf

restart_named_if_reconfigured:
  service.running:
    - name: named-pkcs11
    - failhard: True
    - watch:
        - file: /etc/named/ipa-options-ext.conf

{% endif %}
