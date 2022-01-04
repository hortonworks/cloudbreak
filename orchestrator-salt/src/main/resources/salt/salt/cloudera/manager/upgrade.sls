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

/opt/salt/scripts/check_cmserver_repo_url.sh:
  file.managed:
    - makedirs: True
    - source: salt://cloudera/scripts/check_cm_repo_url.sh.j2
    - template: jinja
    - mode: 700

check_cmserver_repo_url:
  cmd.run:
    - name: /opt/salt/scripts/check_cmserver_repo_url.sh 2>&1 | tee -a /var/log/check_cmserver_repo_url.log && exit ${PIPESTATUS[0]}
    - failhard: True
    - require:
      - file: /opt/salt/scripts/check_cmserver_repo_url.sh

{% endif %}

upgrade-cloudera-server:
  pkg.latest:
    - pkgs:
        - cloudera-manager-server
        - cloudera-manager-daemons
    - refresh: True
    - fromrepo: cloudera-manager
    - failhard: True
    - require:
        - sls: cloudera.repo
