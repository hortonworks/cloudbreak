stop_nm_cloud_setup_timer:
  service.dead:
    - name: nm-cloud-setup.timer
    - enable: False

enable_ip_forwarding:
  sysctl.present:
    - name: net.ipv4.ip_forward
    - value: 1

{%- if pillar.get('forwarder-zones') is defined and pillar.get('forwarder-zones') != None and pillar.get('forwarder-zones', {}).items()|length > 0 %}
replace_resolv_conf_nameservers:
  file.replace:
    - name: /etc/resolv.conf
    - pattern: 'nameserver 127\.0\.0\.1'
    - repl: |
        {%- for forwarder, args in pillar.get('forwarder-zones', {}).items() %}
        {%- for nameserver in args['nameservers'] %}
          nameserver: {{ nameserver }}
        {%- endfor %}
        {%- endfor %}

replace_dhcp_hook_nameservers:
  file.replace:
    - name: /etc/dhcp/dhclient-enter-hooks
    - pattern: 'echo "nameserver 127\.0\.0\.1" >> /etc/resolv\.conf'
    - repl: |
    {%- for forwarder, args in pillar.get('forwarder-zones', {}).items() %}
    {%- for nameserver in args['nameservers'] %}
        echo "nameserver {{ nameserver }}" >> /etc/resolv.conf
    {%- endfor %}
    {%- endfor %}
{%- endif %}

{%- if "ecs" in grains.get('roles', []) %}
stop_nginx:
  service.dead:
    - name: nginx

disable_nginx:
  service.disabled:
    - name: nginx
{%- endif %}