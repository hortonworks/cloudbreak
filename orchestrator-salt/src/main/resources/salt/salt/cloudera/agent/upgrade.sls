{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}

include:
  - postgresql.repo
  - cloudera.repo

stop-cloudera-scm-agent:
  service.dead:
    - name: cloudera-scm-agent

{% if grains['os_family'] == 'RedHat' %}

yum_cleanup_all_before_cm_agent_install:
  cmd.run:
    - name: yum clean all

{% endif %}

upgrade-cloudera-agent:
  pkg.latest:
    - pkgs:
        - cloudera-manager-agent
        - cloudera-manager-daemons
    - refresh: True
    - failhard: True
    - require:
        - sls: cloudera.repo

{% if salt['pillar.get']('cloudera-manager:settings:deterministic_uid_gid') == True %}
inituids_dir_exists:
  file.directory:
    - name: /opt/cloudera/cm-agent/service/inituids
    - user: cloudera-scm
    - group: cloudera-scm
    - dir_mode: 750
    - makedirs: True

set_service_uids_migrate:
  cmd.run:
    - name: /opt/cloudera/cm-agent/service/inituids/set-service-uids.py -m -l DEBUG 2>&1 | tee -a /var/log/set-service-uids.log && [[ 0 -eq ${PIPESTATUS[0]} ]] && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/set-service-uids-executed || exit ${PIPESTATUS[0]}
    - cwd: /opt/cloudera/cm-agent/service/inituids
    - failhard: True
    - onlyif: test -f /opt/cloudera/cm-agent/service/inituids/set-service-uids.py
    - unless: test -f /var/log/set-service-uids-executed
    - require:
        - file: inituids_dir_exists
{% endif %}
