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

{% if salt['pillar.get']('freeipa:hosts') > 1 %}
/cdp/ipahealthagent/freeipa_cluster_node:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/freeipa_cluster_node
{% endif %}

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

/opt/salt/scripts/freeipa_healthagent_setup.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/freeipa_healthagent_setup.sh

/cdp/ipahealthagent/freeipa_healthagent_getcerts.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/freeipa_healthagent_getcerts.sh

/opt/salt/scripts/freeipa_check_replication.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/freeipa_check_replication.sh
