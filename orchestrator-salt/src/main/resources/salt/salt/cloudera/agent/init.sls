{%- from 'metadata/settings.sls' import metadata with context %}
{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}
{%- set manager_server_fqdn = salt['pillar.get']('hosts')[metadata.server_address]['fqdn'] %}

install-cloudera-manager-agent:
  pkg.installed:
    - failhard: True
    - pkgs:
      - cloudera-manager-daemons
      - cloudera-manager-agent

install-psycopg2:
  cmd.run:
    - name: pip install psycopg2==2.7.5 --ignore-installed

replace_server_host:
  file.replace:
    - name: /etc/cloudera-scm-agent/config.ini
    - pattern: "server_host=.*"
    - repl: "server_host={{ manager_server_fqdn }}"
    - unless: grep 'server_host={{ manager_server_fqdn }}' /etc/cloudera-scm-agent/config.ini

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