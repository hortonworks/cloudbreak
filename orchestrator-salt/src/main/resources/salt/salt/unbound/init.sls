{%- from 'metadata/settings.sls' import metadata with context %}
{%- from 'nodes/settings.sls' import host with context %}

faulty_7_2_11_images_unbound_restart_patch:
  file.replace:
    - name: "/etc/dhcp/dhclient-enter-hooks"
    - pattern: "systemctl restart unbound"
    - repl: "pkill -u unbound -SIGHUP unbound"
    - ignore_if_missing: True

{% if salt['pillar.get']('unbound_elimination_supported',False) == True %}
remove_cluster_conf_from_unbound:
  file.absent:
    - name: /etc/unbound/conf.d/00-cluster.conf
{% else %}
/etc/unbound/conf.d/00-cluster.conf:
  file.managed:
    - makedirs: True
    - source: salt://unbound/config/00-cluster.conf
    - template: jinja
    - context:
        server_address: {{ metadata.server_address }}
{% endif %}

/etc/unbound/conf.d/60-domain-dns.conf:
  file.managed:
    - makedirs: True
    - source: salt://unbound/config/60-domain-dns.conf
    - template: jinja
    - context:
      private_address: {{ host.private_address }}

{%- if salt['pillar.get']('default-reverse-zone:nameservers', None) != None %}
/etc/unbound/conf.d/default-reverse-zone.conf:
  file.managed:
    - makedirs: True
    - source: salt://unbound/config/default-reverse-zone.conf
    - template: jinja
{%- endif %}

set_max_ttl:
  file.replace:
    - name: /etc/unbound/unbound.conf
    - pattern: '(#\s)?cache-max-ttl:.*'
    - append_if_not_found: True
    - repl: 'cache-max-ttl: 30'

reload_unbound:
  cmd.run:
    - name: pkill -HUP unbound
    - watch:
      - file: /etc/unbound/conf.d/00-cluster.conf
      - file: /etc/unbound/conf.d/60-domain-dns.conf
{%- if salt['pillar.get']('default-reverse-zone:nameservers', None) != None %}
      - file: /etc/unbound/conf.d/default-reverse-zone.conf
{%- endif %}

unbound:
  service.running:
    - enable: True
    - watch:
      - file: set_max_ttl
