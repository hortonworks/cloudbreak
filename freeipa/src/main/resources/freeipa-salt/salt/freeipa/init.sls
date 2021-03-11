freeipa-install:
  pkg.installed:
    - pkgs:
        - ipa-server
        - ipa-server-dns

net.ipv6.conf.lo.disable_ipv6:
  sysctl.present:
    - value: 0

{% for host in salt['pillar.get']('freeipa:hosts') %}
/etc/hosts/{{ host['fqdn'] }}:
  host.present:
    - ip:
      - {{ host['ip'] }}
    - names:
      - {{ host['fqdn'] }}
{% endfor %}

/opt/salt/scripts/freeipa_install.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/freeipa_install.sh
    - template: jinja

/opt/salt/scripts/freeipa_replica_install.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/freeipa_replica_install.sh
    - template: jinja

/opt/salt/scripts/freeipa_promote_replica_to_master.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/freeipa_promote_replica_to_master.sh
    - template: jinja

/opt/salt/scripts/update_cnames.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/update_cnames.sh

/opt/salt/scripts/repair.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/repair.sh

disable_old_tls_for_ldap_server:
  file.append:
    - name: /usr/share/ipa/updates/20-sslciphers.update
    - text: 'only:sslVersionMin: TLS1.2'

replace_default_tomcat_error_page:
  file.replace:
    - name: /var/lib/pki/pki-tomcat/conf/web.xml
    - pattern: '</web-app>'
    - repl: '<error-page>
             <error-code>404</error-code>
                <location>/error.jsp</location>
             </error-page>
             <error-page>
             <error-code>403</error-code>
                <location>/error.jsp</location>
             </error-page>
             <error-page>
             <error-code>500</error-code>
                <location>/error.jsp</location>
             </error-page>
             </web-app>'
    - backup: False