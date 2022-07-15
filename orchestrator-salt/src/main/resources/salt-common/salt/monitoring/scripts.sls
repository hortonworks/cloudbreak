/opt/salt/scripts/cdp_resources_check.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://monitoring/scripts/cdp_resources_check.sh

/opt/salt/scripts/cert-helper.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://monitoring/scripts/cert-helper.sh

/opt/salt/scripts/monitoring-secret-handler.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://monitoring/scripts/monitoring-secret-handler.sh

/etc/cron.d/cdp_resources_check:
  file.managed:
    - user: root
    - group: root
    - mode: 600
    - source: salt://monitoring/cron/cdp_resources_check

/opt/salt/scripts/instanceid_retriever.py:
  file.managed:
    - user: root
    - group: root
    - mode: 700
    - source: salt://monitoring/scripts/instanceid_retriever.py