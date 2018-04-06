{% if pillar['proxy'] is defined and pillar['proxy']['host'] is defined and pillar['proxy']['port'] is defined and pillar['proxy']['protocol'] is defined %}

{% if grains['os_family'] == 'RedHat' %}
configure_yum_proxy:
  file.append:
    - name: /etc/yum.conf
    - template: jinja
    - source: salt://pkg-mgr-proxy/template/yum_proxy.j2
    - unless: grep "proxy=" /etc/yum.conf
{% elif grains['os_family'] == 'Suse' %}
configure_suse_proxy:
  file.managed:
    - name: /etc/sysconfig/proxy
    - source: salt://pkg-mgr-proxy/template/suse_proxy.j2
{% endif %}

{% endif %}