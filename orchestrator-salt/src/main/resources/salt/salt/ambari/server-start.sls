{%- from 'ambari/settings.sls' import ambari with context %}

copy-optional-jdbc-drivers:
  cmd.run:
    - name: cp -R /opt/jdbc-drivers /usr/share/java
    - onlyif: test -d /opt/jdbc-drivers

{% if ambari.ambari_database.ambariVendor == 'embedded' %}
ambari-start-postgresql:
  service.running:
    - enable: True
    - name: postgresql
{% endif %}

execute-ambari-server-init:
  cmd.run:
    - name: /opt/ambari-server/ambari-server-init.sh 2>&1 | tee -a /var/log/ambari-server-init.log && exit ${PIPESTATUS[0]}
    - unless: test -f /var/ambari-init-executed


{% if not ambari.is_local_ldap and ambari.ldap.certificate is defined and ambari.ldap.certificate is not none %}

/opt/ambari-server/setup-truststore.sh:
  file.managed:
    - makedirs: True
    - source: salt://ambari/scripts/setup-truststore.sh
    - template: jinja
    - context:
      ldap: {{ ambari.ldap }}
      ldaps: {{ ambari.ldaps }}
    - mode: 744

setup-truststore:
  cmd.run:
    - name: /opt/ambari-server/setup-truststore.sh
    - shell: /bin/bash
    - unless: test -f /var/setup_truststore_success

{% endif %}

{% if ambari.is_systemd %}

/etc/systemd/system/ambari-server.service:
  file.managed:
    - source: salt://ambari/systemd/ambari-server.service

start-ambari-server:
  module.wait:
    - name: service.systemctl_reload
    - watch:
      - file: /etc/systemd/system/ambari-server.service
  service.running:
    - enable: True
    - name: ambari-server
    - watch:
       - file: /etc/systemd/system/ambari-server.service

{% else %}

# Upstart case

# Avoid concurrency between SysV and Upstart
disable-ambari-server-sysv:
  cmd.run:
    - name: chkconfig ambari-server off
    - onlyif: chkconfig --list ambari-server | grep on

/etc/init/ambari-server.override:
  file.managed:
    - source: salt://ambari/upstart/ambari-server.override

start-ambari-server:
  service.running:
    - enable: True
    - name: ambari-server

{% endif %}
