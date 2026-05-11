{% set gateway = salt['pillar.get']('gateway') %}

{% if gateway.userfacingcert_configured is defined and gateway.userfacingcert_configured == True %}

/etc/certs-user-facing/server-key.pem:
  file.managed:
    - contents_pillar: gateway:userfacingkey
    - makedirs: True
    - user: root
    - group: root
    - mode: 600
    - require_in:
      - service: restart_nginx_after_ssl_reconfig_with_user_facing

/etc/certs-user-facing/server.pem:
  file.managed:
    - contents_pillar: gateway:userfacingcert
    - makedirs: True
    - user: root
    - group: root
    - mode: 600
    - require_in:
      - service: restart_nginx_after_ssl_reconfig_with_user_facing

{% endif %}

{% if gateway.alternativeuserfacingcert_configured is defined and gateway.alternativeuserfacingcert_configured == True %}

/etc/certs-user-facing/alternative-server-key.pem:
  file.managed:
    - contents_pillar: gateway:alternativeuserfacingkey
    - makedirs: True
    - user: root
    - group: root
    - mode: 600
    - require_in:
      - service: restart_nginx_after_ssl_reconfig_with_user_facing

/etc/certs-user-facing/alternative-server.pem:
  file.managed:
    - contents_pillar: gateway:alternativeuserfacingcert
    - makedirs: True
    - user: root
    - group: root
    - mode: 600
    - require_in:
      - service: restart_nginx_after_ssl_reconfig_with_user_facing

{% endif %}

{% if gateway.userfacingcert_configured is defined or gateway.alternativeuserfacingcert_configured is defined %}
restart_nginx_after_ssl_reconfig_with_user_facing:
  service.running:
    - name: nginx
    - enable: True
    - reload: True
    - watch:
      {% if gateway.userfacingcert_configured is defined and gateway.userfacingcert_configured == True %}
      - file: /etc/certs-user-facing/server-key.pem
      - file: /etc/certs-user-facing/server.pem
      {% endif %}
      {% if gateway.alternativeuserfacingcert_configured is defined and gateway.alternativeuserfacingcert_configured == True %}
      - file: /etc/certs-user-facing/alternative-server-key.pem
      - file: /etc/certs-user-facing/alternative-server.pem
      {% endif %}
{% endif %}