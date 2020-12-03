{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}

include:
  - postgresql.repo
  - cloudera.repo
  - cloudera.csd

stop-cloudera-scm-server:
  service.dead:
    - name: cloudera-scm-server

{% if grains['os_family'] == 'RedHat' %}

yum_cleanup_all_before_cm_server_install:
  cmd.run:
    - name: yum clean all

{% endif %}

upgrade-cloudera-server:
  pkg.latest:
    - pkgs:
        - cloudera-manager-server
        - cloudera-manager-daemons
    - refresh: True
    - failhard: True
    - require:
        - sls: cloudera.repo