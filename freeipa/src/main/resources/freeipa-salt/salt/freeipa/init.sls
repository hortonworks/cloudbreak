freeipa-install:
  pkg.installed:
    - failhard: True
    - pkgs:
        - ipa-server
        - ipa-server-dns

net.ipv6.conf.lo.disable_ipv6:
  sysctl.present:
    - value: 0

{% for host in salt['pillar.get']('freeipa:hosts') %}
{{ host['ip'] }}:
  host.only:
    - hostnames:
      - {{ host['fqdn'] }}
{% if '.' in host['fqdn'] %}
      - {{ host['fqdn'].split('.')[0] }}
{% endif %}
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

/opt/salt/scripts/update_cnames.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/update_cnames.sh
    - template: jinja

/opt/salt/scripts/repair.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/repair.sh

{%- if salt['pillar.get']('freeipa:hosts') %}
/root/.config/checkipaconsistency:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 600
    - show_changes: False
    - source: salt://freeipa/templates/checkipaconsistency.j2
    - template: jinja
{%- endif %}

disable_old_tls_for_ldap_server:
  file.append:
    - name: /usr/share/ipa/updates/20-sslciphers.update
    - text: 'only:sslVersionMin: TLS1.2'

/opt/salt/scripts/initdnarange.py:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/initdnarange.py

/opt/salt/initial-ldap-conf.ldif:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 600
    - source: salt://freeipa/templates/initial-ldap-conf.ldif
    - template: jinja
