{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}

include:
  - cloudera.repo

stop-cloudera-scm-server:
  service.dead:
    - name: cloudera-scm-server

upgrade-cloudera-server:
  pkg.latest:
    - pkgs:
        - cloudera-manager-server
        - cloudera-manager-daemons
    - refresh: True
    - failhard: True
    - require:
        - sls: cloudera.repo