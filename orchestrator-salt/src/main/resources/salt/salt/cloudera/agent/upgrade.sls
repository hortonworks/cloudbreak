{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}

include:
  - cloudera.repo

stop-cloudera-scm-agent:
  service.dead:
    - name: cloudera-scm-agent

upgrade-cloudera-agent:
  pkg.latest:
    - pkgs:
        - cloudera-manager-agent
        - cloudera-manager-daemons
    - refresh: True
    - require:
        - sls: cloudera.repo