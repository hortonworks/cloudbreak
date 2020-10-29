{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}

include:
  - postgresql.repo
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
    - failhard: True
    - require:
        - sls: cloudera.repo

{%- if salt['pillar.get']('cloudera-manager:settings:deterministic_uid_gid') == True %}
{%- if "knox" in grains.get('roles', []) %}
  {% set procName = 'KNOX_GATEWAY' %}
{%- elif "idbroker" in grains.get('roles', []) %}
  {% set procName = 'IDBROKER' %}
{%- endif %}

{%- if procName is defined and procName != None %}
force-stop-knox:
  cmd.script:
    - source: salt://cloudera/agent/scripts/force-stop-knox.sh
    - env:
        - PROC_NAME: {{ procName }}
{%- endif %}

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

{%- if procName is defined and procName != None %}
ensure-cloudera-scm-agent-stopped:
  service.dead:
    - name: cloudera-scm-agent

start-knox:
  cmd.script:
    - source: salt://cloudera/agent/scripts/start-knox.sh
{%- endif %}
{% endif %}
