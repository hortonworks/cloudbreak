{%- from 'metadata/settings.sls' import metadata with context %}
{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}
{%- set manager_server_fqdn = salt['pillar.get']('hosts')[metadata.server_address]['fqdn'] %}
{%- set loadbalancer_url = salt['pillar.get']('cloudera-manager:communication:loadbalancer_url') %}
{%- set primary_gateway = salt['pillar.get']('cloudera-manager:communication:primary_fqdn') %}

install-cloudera-manager-agent:
  pkg.installed:
    - failhard: True
    - pkgs:
      - cloudera-manager-daemons
      - cloudera-manager-agent
    - unless:
      - rpm -q cloudera-manager-daemons cloudera-manager-agent

{% if cloudera_manager.settings.cloudera_scm_sudo_access == True %}

/etc/sudoers.d/cloudera-scm:
  file.managed:
    - contents:
      - cloudera-scm ALL=(ALL) NOPASSWD:ALL
    - user: root
    - group: root
    - mode: 440

{% endif %}

{%- if not salt['pkg.version']('python-psycopg2') and not salt['pkg.version']('python2-psycopg2') %}
install-psycopg2:
  cmd.run:
    - name: pip install psycopg2==2.7.5 --ignore-installed
    - unless: pip list --no-index | grep -E 'psycopg2.*2.7.5'
{%- endif %}

replace_server_host:
  file.replace:
    - name: /etc/cloudera-scm-agent/config.ini
    - pattern: "server_host=.*"
    - repl: "server_host={{ primary_gateway }}"
    - unless: grep 'server_host={{ primary_gateway }}' /etc/cloudera-scm-agent/config.ini

{% if cloudera_manager.communication.autotls_enabled == True %}

setup_autotls_token:
  file.line:
    - name: /etc/cloudera-scm-agent/config.ini
    - mode: ensure
    - content: "cert_request_token_file=/etc/cloudera-scm-agent/cmagent.token"
    - after: "use_tls=.*"
    - backup: False

{% endif %}

/opt/scripts/generate-host-id.sh:
  file.managed:
    - makedirs: True
    - source: salt://cloudera/agent/scripts/generate-host-id.sh
    - mode: 744

generate_host_id:
  cmd.run:
    - name: /opt/scripts/generate-host-id.sh
    - shell: /bin/bash

{% if salt['pillar.get']('cloudera-manager:settings:deterministic_uid_gid') == True and not "manager_upgrade" in grains.get('roles', []) %}
/opt/cloudera/cm-agent/service/inituids:
  file.directory:
    - user: cloudera-scm
    - group: cloudera-scm
    - dir_mode: 750
    - makedirs: True

set_service_uids:
  cmd.run:
    - name: /opt/cloudera/cm-agent/service/inituids/set-service-uids.py -l INFO  2>&1 | tee -a /var/log/set-service-uids.log && [[ 0 -eq ${PIPESTATUS[0]} ]] && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/set-service-uids-executed || exit ${PIPESTATUS[0]}
    - cwd: /opt/cloudera/cm-agent/service/inituids
    - failhard: True
    - onlyif: test -f /opt/cloudera/cm-agent/service/inituids/set-service-uids.py
    - unless: test -f /var/log/set-service-uids-executed
    - require:
        - file: /opt/cloudera/cm-agent/service/inituids
{% endif %}