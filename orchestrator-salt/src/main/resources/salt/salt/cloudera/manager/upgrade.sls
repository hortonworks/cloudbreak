{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}

include:
  - postgresql.repo
  - cloudera.repo
  - cloudera.csd

stop-cloudera-scm-server-during-upgrade:
  service.dead:
    - name: cloudera-scm-server

upgrade-cloudera-server:
  pkg.latest:
    - pkgs:
        - cloudera-manager-server
        - cloudera-manager-daemons
    - refresh: True
    - fromrepo: cloudera-manager-{{ salt['pillar.get']('cloudera-manager:repo:version') }}-{{ salt['pillar.get']('cloudera-manager:repo:buildNumber') }}
    - failhard: True
    - require:
        - sls: cloudera.repo
