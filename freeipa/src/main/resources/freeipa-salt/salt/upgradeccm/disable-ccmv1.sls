rename_reverse_tunnel_values_gateway_script:
  file.rename:
    - name: /cdp/bin/reverse-tunnel-values-GATEWAY.sh.bak
    - source: /cdp/bin/reverse-tunnel-values-GATEWAY.sh
    - force: True
    - makedirs: True

/etc/cron.hourly/socket-wait-cleanup.sh:
  file.managed:
    - user: root
    - group: root
    - mode: 640

stop_ccm_v1_service:
  service.dead:
    - enable: False
    - name: ccm-tunnel@GATEWAY
