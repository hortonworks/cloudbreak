start_agent:
  service.running:
    - enable: True
    - name: cloudera-scm-agent
