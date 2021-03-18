{%- from 'gateway/settings.sls' import gateway with context %}

# This file contains the intial config for Knox CSD

{{ gateway.knox_data_root }}/topologies:
  file.directory:
    - mode: 755
    - makedirs: True

{{ gateway.knox_data_root }}/topologies/admin.xml:
  file.managed:
    - source: salt://gateway/config/cm/admin.xml.j2
    - template: jinja
    - mode: 644
    - context:
        ldap: {{ gateway.ldap }}

{{ gateway.knox_data_root }}/topologies/manager.xml:
  file.managed:
    - source: salt://gateway/config/cm/manager.xml.j2
    - template: jinja
    - mode: 644
    - context:
        ldap: {{ gateway.ldap }}

{{ gateway.knox_data_root }}/topologies/knoxsso.xml:
  file.managed:
    - source: salt://gateway/config/cm/knoxsso.xml.j2
    - template: jinja
    - mode: 644

{% if salt['pillar.get']('gateway:tokencert') != None %}
{{ gateway.knox_data_root }}/topologies/cdp-token.xml:
  file.managed:
    - source: salt://gateway/config/cm/cdp-token.xml.j2
    - template: jinja
    - mode: 644
{% endif %}

{% for topology in salt['pillar.get']('gateway:topologies') -%}

{{ gateway.knox_data_root }}/topologies/{{ topology.name }}.xml:
  file.managed:
    - source: salt://gateway/config/cm/topology.xml.j2
    - template: jinja
    - context:
      exposed: {{ topology.exposed }}
      ports: {{ salt['pillar.get']('gateway:ports') }}
      topology_name: {{ topology.name }}
      protocol: {{ salt['pillar.get']('gateway:protocol') }}
    - mode: 644

{{ gateway.knox_data_root }}/topologies/{{ topology.name }}-api.xml:
  file.managed:
    - source: salt://gateway/config/cm/topology_api.xml.j2
    - template: jinja
    - context:
      exposed: {{ topology.exposed }}
      ports: {{ salt['pillar.get']('gateway:ports') }}
      topology_name: {{ topology.name }}
      protocol: {{ salt['pillar.get']('gateway:protocol') }}
    - mode: 644

{{ gateway.knox_data_root }}/topologies/{{ topology.name }}-token.xml:
  file.managed:
    - source: salt://gateway/config/cm/topology_token.xml.j2
    - template: jinja
    - context:
      exposed: {{ topology.exposed }}
      ports: {{ salt['pillar.get']('gateway:ports') }}
      topology_name: {{ topology.name }}
      protocol: {{ salt['pillar.get']('gateway:protocol') }}
    - mode: 644

{% endfor %}

{{ gateway.knox_data_root }}/security/keystores/signkey.pem:
  file.managed:
    - contents_pillar: gateway:signkey
    - makedirs: True
{% if salt['pillar.get']('cloudera-manager:settings:deterministic_uid_gid') == True %}
    - user: knox
    - group: knox
    - mode: 640
{% else %}
    - mode: 644
{% endif %}

{{ gateway.knox_data_root }}/security/keystores/signcert.pem:
  file.managed:
    - contents_pillar: gateway:signcert
    - makedirs: True
{% if salt['pillar.get']('cloudera-manager:settings:deterministic_uid_gid') == True %}
    - user: knox
    - group: knox
    - mode: 640
{% else %}
    - mode: 644
{% endif %}

  # openssl pkcs12 -export -in cert.pem -inkey key.pem -out signing.p12 -name signing-identity -password pass:admin
  # keytool -importkeystore -deststorepass admin1 -destkeypass admin1 -destkeystore signing.jks -srckeystore signing.p12 -srcstoretype PKCS12 -srcstorepass admin -alias signing-identity

knox-create-sign-pkcs12:
  cmd.run:
    - name: cd {{ gateway.knox_data_root }}/security/keystores/ && openssl pkcs12 -export -in signcert.pem -inkey signkey.pem -out signing.p12 -name signing-identity -password pass:{{ salt['pillar.get']('gateway:mastersecret') }}
    - creates: {{ gateway.knox_data_root }}/security/keystores/signing.p12
    - output_loglevel: quiet

{{ gateway.knox_data_root }}/security/keystores/signing.p12:
  file.managed:
{% if salt['pillar.get']('cloudera-manager:settings:deterministic_uid_gid') == True %}
    - user: knox
    - group: knox
    - mode: 640
{% else %}
    - mode: 644
{% endif %}
    - replace: False

knox-create-sign-jks:
  cmd.run:
    - name: cd {{ gateway.knox_data_root }}/security/keystores/ && keytool -importkeystore -deststorepass {{ salt['pillar.get']('gateway:mastersecret') }} -destkeypass {{ salt['pillar.get']('gateway:mastersecret') }} -destkeystore signing.jks -srckeystore signing.p12 -srcstoretype PKCS12 -srcstorepass {{ salt['pillar.get']('gateway:mastersecret') }} -alias signing-identity
    - creates: {{ gateway.knox_data_root }}/security/keystores/signing.jks
    - output_loglevel: quiet

{{ gateway.knox_data_root }}/security/keystores/signing.jks:
  file.managed:
{% if salt['pillar.get']('cloudera-manager:settings:deterministic_uid_gid') == True %}
    - user: knox
    - group: knox
    - mode: 640
{% else %}
    - mode: 644
{% endif %}
    - replace: False

{% if salt['pillar.get']('gateway:saml') %}

{{ gateway.knox_data_root }}/cdp-idp-metadata.xml:
  file.managed:
    - contents_pillar: gateway:saml
    - mode: 644
    - replace: False

{% endif %}
