{% if pillar['proxy'] is defined and pillar['proxy']['host'] is defined and pillar['proxy']['port'] is defined and pillar['proxy']['protocol'] is defined %}
configure_yum_proxy:
  file.append:
    - name: /etc/yum.conf
    - template: jinja
    - source: salt://yum-proxy/template/proxy.j2
    - unless: grep "proxy=" /etc/yum.conf
{% endif %}
