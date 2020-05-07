freeipa-install:
  pkg.installed:
    - pkgs:
        - ntp
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

/opt/salt/scripts/update_cnames.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/update_cnames.sh