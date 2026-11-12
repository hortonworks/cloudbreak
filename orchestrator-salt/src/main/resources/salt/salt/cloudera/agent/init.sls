{%- from 'metadata/settings.sls' import metadata with context %}
{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}
{%- from 'nodes/settings.sls' import host with context %}
{%- set manager_server_fqdn = salt['pillar.get']('hosts')[metadata.server_address]['fqdn'] %}
{%- set internal_loadbalancer_san = salt['pillar.get']('cloudera-manager:communication:internal_loadbalancer_san') %}
{%- set cpuarch = salt['grains.get']('cpuarch') %}

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

# The package we check for is called differently for different OS + Python version 
# combinations and we need to be careful to keep backward compatibility.
# 1. CentOS 7 + Python 2
# 2. RedHat 7 + Python 2 (customer specific)
# 3. RedHat 8 + Python 3.8
# 4. CentOS 8 + Python 3.8
{%- if cpuarch != 'aarch64'
   and not salt['pkg.version']('python-psycopg2')
   and not salt['pkg.version']('python2-psycopg2')
   and not salt['pkg.version']('python38-psycopg2')
   and not salt['pkg.version']('rh-python38-python-psycopg2')
%}
install-psycopg2:
  cmd.run:
    - name: pip install psycopg2==2.7.5 --ignore-installed
    - unless: pip list --no-index | grep -E 'psycopg2'
{%- endif %}

replace_server_host:
  file.replace:
    - name: /etc/cloudera-scm-agent/config.ini
    - pattern: "server_host=.*"
    - repl: "server_host={{ manager_server_fqdn }}"
    - unless: grep 'server_host={{ manager_server_fqdn }}' /etc/cloudera-scm-agent/config.ini

# Pin the agent's reported hostname to the Cloudbreak-authoritative FQDN so it never
# self-reports a short name when FQDN resolution is not ready at agent start.
pin_reported_hostname:
  file.replace:
    - name: /etc/cloudera-scm-agent/config.ini
    - pattern: '^#*\s*reported_hostname=.*'
    - repl: 'reported_hostname={{ host.fqdn }}'
    - append_if_not_found: True
    - backup: False

{% if salt['pillar.get']('platform') == 'GCP' %}
# The guest agent's ExecStartPost (cleanup-hosts.sh) rewrites /etc/hosts non-atomically. An agent starting during the
# rewrite, while FreeIPA DNS is not answering yet, caches the short hostname and the first local IPv4 (a lo VIP on
# load balancer backends). The guest agent is active only after its ExecStartPost finished.
/etc/systemd/system/cloudera-scm-agent.service.d/after-guest-agent.conf:
  file.managed:
    - makedirs: True
    - contents: |
        [Unit]
        After=google-guest-agent.service
    - mode: 644

systemctl-reload-on-cloudera-scm-agent-after-guest-agent-change:
  cmd.run:
    - name: systemctl --system daemon-reload
    - onchanges:
      - file: /etc/systemd/system/cloudera-scm-agent.service.d/after-guest-agent.conf
{% endif %}

{% if cloudera_manager.communication.autotls_enabled == True %}

setup_autotls_token:
  file.line:
    - name: /etc/cloudera-scm-agent/config.ini
    - mode: ensure
    - content: "cert_request_token_file=/etc/cloudera-scm-agent/cmagent.token"
    - after: "use_tls=.*"
    - backup: False
    - unless: grep -q "^cert_request_token_file=" /etc/cloudera-scm-agent/config.ini

{% if internal_loadbalancer_san|length > 0 %}

replace_autotls_san:
  file.replace:
    - name: /etc/cloudera-scm-agent/config.ini
    - pattern: "^subject_alt_names=.*"
    - repl: "subject_alt_names={{ internal_loadbalancer_san }}"
    - backup: False

setup_autotls_san:
  file.line:
    - name: /etc/cloudera-scm-agent/config.ini
    - mode: ensure
    - content: "subject_alt_names={{ internal_loadbalancer_san }}"
    - after: "^cert_request_token_file=.*"
    - backup: False
    - quiet: True
    - unless: grep -q "^subject_alt_names=" /etc/cloudera-scm-agent/config.ini

{% endif %}

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

