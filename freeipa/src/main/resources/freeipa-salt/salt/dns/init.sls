dhcp-enter-hook:
  file.managed:
    - name: /etc/dhcp/dhclient-enter-hooks
    - source: salt://dns/scripts/nodnsupdate
    - makedirs: True
    - user: root
    - group: root
    - mode: 755

set-peerdns-script:
  file.managed:
    - name: /opt/salt/scripts/set_peerdns.sh
    - source: salt://dns/scripts/set_peerdns.sh
    - makedirs: True
    - user: root
    - group: root
    - mode: 755

run-set-peerdns:
  cmd.run:
    - name: /opt/salt/scripts/set_peerdns.sh
    - require:
        - file: set-peerdns-script

{% if salt['pkg.version']('NetworkManager') %}

nm-nodns-config:
  file.managed:
    - name: /etc/NetworkManager/conf.d/nodnsupdate.conf
    - source: salt://dns/conf/nodnsupdate.conf
    - user: root
    - group: root
    - mode: 740
    - makedirs: true

restart-nm:
  service.running:
    - name: NetworkManager
    - watch:
        - file: dhcp-enter-hook
        - file: nm-nodns-config
        - cmd: run-set-peerdns

{% endif %}