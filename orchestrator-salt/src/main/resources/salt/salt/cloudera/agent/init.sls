{%- from 'metadata/settings.sls' import metadata with context %}

install-cloudera-manager-agent:
  pkg.installed:
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
    - repl: "server_host=cluster-manager.{{ metadata.cluster_domain }}"
    - unless: cat /etc/cloudera-scm-agent/config.ini | grep server_host=host-10-0-0-3.openstacklocal

start_agent:
  service.running:
    - enable: True
    - name: cloudera-scm-agent
