{% if salt['pillar.get']('platform') != 'AWS' or salt['pillar.get']('hosts')[salt['network.interface_ip']('eth0')]['custom_domain'] %}
{% for ip, args in pillar.get('hosts', {}).items() %}
replace_etc_hosts_{{ loop.index }}:
  file.replace:
    - name: /etc/hosts
    - pattern: "{{ ip }}\\s *.*"
    - repl: "{{ ip }} {{ args['fqdn'] }} {{ args['hostname'] }}"
    - append_if_not_found: true
{% endfor %}
{% endif %}