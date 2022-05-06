rename_reverse_tunnel_values_gateway_script:
  file.rename:
    - name: /cdp/bin/reverse-tunnel-values-GATEWAY.sh.bak
    - source: /cdp/bin/reverse-tunnel-values-GATEWAY.sh
    - force: True
    - makedirs: True

rename_reverse_tunnel_values_knox_script:
  file.rename:
    - name: /cdp/bin/reverse-tunnel-values-KNOX.sh.bak
    - source: /cdp/bin/reverse-tunnel-values-KNOX.sh
    - force: True
    - makedirs: True

/etc/cron.hourly/socket-wait-cleanup.sh:
  file.managed:
    - user: root
    - group: root
    - mode: 640

stop_ccm_v1_service_gateway:
  service.dead:
    - enable: False
    - name: ccm-tunnel@GATEWAY

stop_ccm_v1_service_knox:
  service.dead:
    - enable: False
    - name: ccm-tunnel@KNOX
