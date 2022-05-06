stop_ccm_v2_agent:
  service.dead:
    - enable: False
    - name: jumpgate-agent
