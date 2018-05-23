{%- from 'gateway/settings.sls' import gateway with context %}

include:
{% if grains['os_family'] == 'RedHat' %}
  - gateway.repo
{% endif %}
{% if grains['os_family'] == 'Debian' %}
  - gateway.repo-debian
{% endif %}
{% if grains['os_family'] == 'Suse' %}
  - gateway.repo-suse
{% endif %}

knox:
  pkg.installed

/var/run/knox:
  file.directory:
    - user: knox
    - group: knox
    - mode: 755
    - makedirs: True

#/usr/hdp/current/knox-server/conf/topologies/admin.xml:
#  file.absent

/usr/hdp/current/knox-server/conf/topologies/manager.xml:
  file.absent

knox-master-secret:
  cmd.run:
    - name: /usr/hdp/current/knox-server/bin/knoxcli.sh create-master --master '{{ salt['pillar.get']('gateway:mastersecret') }}'
    - user: knox
    - creates: /usr/hdp/current/knox-server/data/security/master
    - output_loglevel: quiet

knox-create-cert:
  cmd.run:
    - name: /usr/hdp/current/knox-server/bin/knoxcli.sh create-cert --hostname {{ salt['grains.get']('gateway-address')[0] }}
    - user: knox
    - creates: /usr/hdp/current/knox-server/data/security/keystores/gateway.jks

/usr/hdp/current/knox-server/data/security/keystores/signkey.pem:
  file.managed:
    - user: knox
    - group: hadoop
    - contents_pillar: gateway:signkey

/usr/hdp/current/knox-server/data/security/keystores/signcert.pem:
  file.managed:
    - user: knox
    - group: hadoop
    - contents_pillar: gateway:signcert

# openssl pkcs12 -export -in cert.pem -inkey key.pem -out signing.p12 -name signing-identity -password pass:admin
# keytool -importkeystore -deststorepass admin1 -destkeypass admin1 -destkeystore signing.jks -srckeystore signing.p12 -srcstoretype PKCS12 -srcstorepass admin -alias signing-identity

knox-create-sign-pkcs12:
  cmd.run:
    - name: cd /usr/hdp/current/knox-server/data/security/keystores/ && openssl pkcs12 -export -in signcert.pem -inkey signkey.pem -out signing.p12 -name signing-identity -password pass:{{ salt['pillar.get']('gateway:mastersecret') }}
    - user: knox
    - creates: /usr/hdp/current/knox-server/data/security/keystores/signing.p12
    - output_loglevel: quiet

knox-create-sign-jks:
  cmd.run:
    - name: cd /usr/hdp/current/knox-server/data/security/keystores/ && keytool -importkeystore -deststorepass {{ salt['pillar.get']('gateway:mastersecret') }} -destkeypass {{ salt['pillar.get']('gateway:mastersecret') }} -destkeystore signing.jks -srckeystore signing.p12 -srcstoretype PKCS12 -srcstorepass {{ salt['pillar.get']('gateway:mastersecret') }} -alias signing-identity
    - user: knox
    - creates: /usr/hdp/current/knox-server/data/security/keystores/signing.jks
    - output_loglevel: quiet

#knox-export-cert:
#  cmd.run:
#    - name: /usr/hdp/current/knox-server/bin/knoxcli.sh export-cert --type PEM
#    - user: knox
#    - creates: /usr/hdp/current/knox-server/data/security/keystores/gateway-identity.pem

 # openssl x509 -in /usr/hdp/current/knox-server/data/security/keystores/gateway-identity.pem -text -noout

{% if gateway.is_local_ldap %}

/usr/hdp/current/knox-server/conf/users.ldif:
  file.managed:
    - source: salt://gateway/config/users.ldif.j2
    - template: jinja

{% endif %}

/usr/hdp/current/knox-server/conf/gateway-site.xml:
  file.managed:
    - source: salt://gateway/config/gateway-site.xml.j2
    - template: jinja

/var/lib/ambari-server/resources/common-services/KNOX/0.5.0.2.2/configuration/gateway-site.xml:
  file.managed:
    - source: salt://gateway/config/gateway-site.xml.j2
    - template: jinja

{% for topology in salt['pillar.get']('gateway:topologies') -%}

/usr/hdp/current/knox-server/conf/topologies/{{ topology.name }}.xml:
  file.managed:
    - source: salt://gateway/config/topology.xml.j2
    - template: jinja
    - context:
      exposed: {{ topology.exposed }}
    - user: knox
    - group: knox

{% endfor %}

{% if 'SSO_PROVIDER' == salt['pillar.get']('gateway:ssotype') %}

/usr/hdp/current/knox-server/conf/topologies/sso.xml:
  file.managed:
    - source: salt://gateway/config/knoxsso.xml.j2
    - template: jinja
    - user: knox
    - group: knox

{% if salt['pillar.get']('gateway:tokencert') != None %}
/usr/hdp/current/knox-server/conf/topologies/token.xml:
  file.managed:
    - source: salt://gateway/config/token.xml.j2
    - template: jinja
    - user: knox
    - group: knox

{% endif %}

{% endif %}

/usr/hdp/current/knox-server/conf/topologies/knoxsso.xml:
  file.absent

{% if gateway.is_systemd %}

/usr/lib/tmpfiles.d/knox.conf:
  file.managed:
    - source: salt://gateway/systemd/knox.conf

{% if gateway.is_local_ldap %}

/etc/systemd/system/knox-ldap.service:
  file.managed:
    - source: salt://gateway/systemd/knox-ldap.service

start-knox-ldap:
  module.wait:
    - name: service.systemctl_reload
    - watch:
      - file: /etc/systemd/system/knox-ldap.service
  service.running:
    - enable: True
    - name: knox-ldap
    - watch:
       - file: /etc/systemd/system/knox-ldap.service

{% endif %}

/etc/systemd/system/knox-gateway.service:
  file.managed:
    - source: salt://gateway/systemd/knox-gateway.service

start-knox-gateway:
  module.wait:
    - name: service.systemctl_reload
    - watch:
      - file: /etc/systemd/system/knox-gateway.service
  service.running:
    - enable: True
    - name: knox-gateway
    - watch:
       - file: /etc/systemd/system/knox-gateway.service


{% else %}

# Upstart case
{% if gateway.is_local_ldap %}

/usr/hdp/current/knox-server/bin/ldap.sh:
  file.managed:
    - source: salt://gateway/upstart/ldap.sh
    - mode: 755

/etc/init/knox-ldap.conf:
  file.managed:
    - source: salt://gateway/upstart/knox-ldap.conf

start-knox-ldap:
  service.running:
    - enable: True
    - name: knox-ldap

{% endif %}

/usr/hdp/current/knox-server/bin/gateway.sh:
  file.managed:
    - source: salt://gateway/upstart/gateway.sh
    - mode: 755

/etc/init/knox-gateway.conf:
  file.managed:
    - source: salt://gateway/upstart/knox-gateway.conf

start-knox-gateway:
  service.running:
    - enable: True
    - name: knox-gateway

{% endif %}

{% if salt['pillar.get']('hdp:stack:vdf-url') != None %}

add_vdf_parse_script_agent:
  file.managed:
    - name: /opt/salt/extract-repo-url-from-vdf.sh
    - source: salt://gateway/yum/scripts/extract-repo-url-from-vdf.sh
    - skip_verify: True
    - makedirs: True
    - mode: 755

run_vdf_parse_script_agent:
  cmd.run:
    - name: sh -x /opt/salt/extract-repo-url-from-vdf.sh {{ salt['pillar.get']('hdp:stack:vdf-url') }} | tee -a /var/log/add_vdf_parse_script_agent.log && exit ${PIPESTATUS[0]}
    - unless: ls /var/log/add_vdf_parse_script_agent.log
    - require:
      - file: add_vdf_parse_script_agent

{% endif %}
