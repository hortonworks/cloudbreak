{%- from 'gateway/settings.sls' import gateway with context %}

add_knox_settings_to_cm:
  file.append:
    - name: /etc/cloudera-scm-server/cm.settings
    - makedirs: True
    - template: jinja
    - source: salt://gateway/config/cm/knox.settings.j2
    - unless: grep "PROXYUSER_KNOX_GROUPS" /etc/cloudera-scm-server/cm.settings

cloudera_manager_setup_knox:
  file.replace:
    - name: /etc/default/cloudera-scm-server
    - pattern: "CMF_SERVER_ARGS=.*"
    - repl: CMF_SERVER_ARGS="-i /etc/cloudera-scm-server/cm.settings"
    - unless: grep "CMF_SERVER_ARGS=\"-i /etc/cloudera-scm-server/cm.settings\"" /etc/default/cloudera-scm-server

{{ gateway.knox_data_root }}/topologies:
  file.directory:
    - mode: 777
    - makedirs: True

{{ gateway.knox_data_root }}/topologies/admin.xml:
  file.managed:
    - source: salt://gateway/config/cm/admin.xml.j2
    - template: jinja
    - mode: 777
    - context:
        ldap: {{ gateway.ldap }}

{{ gateway.knox_data_root }}/topologies/manager.xml:
  file.managed:
    - source: salt://gateway/config/cm/manager.xml.j2
    - template: jinja
    - mode: 777
    - context:
        ldap: {{ gateway.ldap }}

{{ gateway.knox_data_root }}/topologies/knoxsso.xml:
  file.managed:
    - source: salt://gateway/config/cm/knoxsso.xml.j2
    - template: jinja
    - mode: 777

{{ gateway.knox_data_root }}/topologies/sandbox.xml:
  file.managed:
    - source: salt://gateway/config/cm/sandbox.xml.j2
    - template: jinja
    - mode: 777

{% for topology in salt['pillar.get']('gateway:topologies') -%}

{{ gateway.knox_data_root }}/topologies/{{ topology.name }}.xml:
  file.managed:
    - source: salt://gateway/config/cm/topology.xml.j2
    - template: jinja
    - context:
      exposed: {{ topology.exposed }}
      ports: {{ salt['pillar.get']('gateway:ports') }}
      topology_name: {{ topology.name }}
    - mode: 777

{% endfor %}

{{ gateway.knox_data_root }}/security/keystores/signkey.pem:
  file.managed:
    - contents_pillar: gateway:signkey
    - makedirs: True
    - mode: 777

{{ gateway.knox_data_root }}/security/keystores/signcert.pem:
  file.managed:
    - contents_pillar: gateway:signcert
    - makedirs: True
    - mode: 777

  # openssl pkcs12 -export -in cert.pem -inkey key.pem -out signing.p12 -name signing-identity -password pass:admin
  # keytool -importkeystore -deststorepass admin1 -destkeypass admin1 -destkeystore signing.jks -srckeystore signing.p12 -srcstoretype PKCS12 -srcstorepass admin -alias signing-identity

knox-create-sign-pkcs12:
  cmd.run:
    - name: cd {{ gateway.knox_data_root }}/security/keystores/ && openssl pkcs12 -export -in signcert.pem -inkey signkey.pem -out signing.p12 -name signing-identity -password pass:{{ salt['pillar.get']('gateway:mastersecret') }}
    - creates: {{ gateway.knox_data_root }}/security/keystores/signing.p12
    - output_loglevel: quiet

{{ gateway.knox_data_root }}/security/keystores/signing.p12:
  file.managed:
    - mode: 777
    - replace: False

knox-create-sign-jks:
  cmd.run:
    - name: cd {{ gateway.knox_data_root }}/security/keystores/ && keytool -importkeystore -deststorepass {{ salt['pillar.get']('gateway:mastersecret') }} -destkeypass {{ salt['pillar.get']('gateway:mastersecret') }} -destkeystore signing.jks -srckeystore signing.p12 -srcstoretype PKCS12 -srcstorepass {{ salt['pillar.get']('gateway:mastersecret') }} -alias signing-identity
    - creates: {{ gateway.knox_data_root }}/security/keystores/signing.jks
    - output_loglevel: quiet

{{ gateway.knox_data_root }}/security/keystores/signing.jks:
  file.managed:
    - mode: 777
    - replace: False