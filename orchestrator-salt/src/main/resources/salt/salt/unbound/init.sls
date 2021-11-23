{%- from 'metadata/settings.sls' import metadata with context %}
{%- from 'nodes/settings.sls' import host with context %}

faulty_7_2_11_images_unbound_restart_patch:
  file.replace:
    - name: "/etc/dhcp/dhclient-enter-hooks"
    - pattern: "systemctl restart unbound"
    - repl: "pkill -u unbound -SIGHUP unbound"
    - ignore_if_missing: True

/etc/unbound/conf.d/00-cluster.conf:
  file.managed:
    - makedirs: True
    - source: salt://unbound/config/00-cluster.conf
    - template: jinja
    - context:
        server_address: {{ metadata.server_address }}

/etc/dhcp/dhclient.d/google_hostname.sh:
  file.managed:
    - makedirs: True
    - source: salt://unbound/dhcp/google_hostname.sh
    - mode: 744

/etc/unbound/conf.d/60-domain-dns.conf:
  file.managed:
    - makedirs: True
    - source: salt://unbound/config/60-domain-dns.conf
    - template: jinja
    - context:
      private_address: {{ host.private_address }}

include_access_config:
  file.replace:
    - name: /etc/unbound/unbound.conf
    - pattern: '#include: "otherfile.conf"'
    - repl: 'include: "/etc/unbound/access.conf"'
    - unless: grep "/etc/unbound/access.conf" /etc/unbound/unbound.conf

/etc/unbound/access.conf:
  file.managed:
    - source: salt://unbound/config/access.conf

enable_auto_interface:
  file.replace:
    - name: /etc/unbound/unbound.conf
    - pattern: "  interface-automatic: no"
    - repl: "  interface-automatic: yes"

reload_unbound:
  cmd.run:
    - name: pkill -HUP unbound
    - watch:
      - file: /etc/unbound/conf.d/00-cluster.conf
      - file: /etc/unbound/conf.d/60-domain-dns.conf

unbound:
  service.running:
    - enable: True
    - watch:
      - file: enable_auto_interface
      - file: /etc/unbound/access.conf