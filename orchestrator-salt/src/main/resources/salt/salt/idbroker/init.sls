{% if salt['pillar.get']('idbroker:mastersecret', None) != None %}
{%- from 'idbroker/settings.sls' import idbroker with context %}

{{ idbroker.knox_data_root }}/security/keystores/signkey.pem:
  file.managed:
    - contents_pillar: idbroker:signkey
    - makedirs: True
    - mode: 644

{{ idbroker.knox_data_root }}/security/keystores/signcert.pem:
  file.managed:
    - contents_pillar: idbroker:signcert
    - makedirs: True
    - mode: 644

# openssl pkcs12 -export -in cert.pem -inkey key.pem -out signing.p12 -name signing-identity -password pass:admin
# keytool -importkeystore -deststorepass admin1 -destkeypass admin1 -destkeystore signing.jks -srckeystore signing.p12 -srcstoretype PKCS12 -srcstorepass admin -alias signing-identity

idbroker-create-sign-pkcs12:
  cmd.run:
    - name: cd {{ idbroker.knox_data_root }}/security/keystores/ && openssl pkcs12 -export -in signcert.pem -inkey signkey.pem -out signing.p12 -name signing-identity -password pass:{{ salt['pillar.get']('idbroker:mastersecret') }}
    - creates: {{ idbroker.knox_data_root }}/security/keystores/signing.p12
    - output_loglevel: debug

{{ idbroker.knox_data_root }}/security/keystores/signing.p12:
  file.managed:
    - mode: 644
    - replace: False

idbroker-create-sign-jks:
  cmd.run:
    - name: cd {{ idbroker.knox_data_root }}/security/keystores/ && keytool -importkeystore -deststorepass {{ salt['pillar.get']('idbroker:mastersecret') }} -destkeypass {{ salt['pillar.get']('idbroker:mastersecret') }} -destkeystore signing.jks -srckeystore signing.p12 -srcstoretype PKCS12 -srcstorepass {{ salt['pillar.get']('idbroker:mastersecret') }} -alias signing-identity
    - creates: {{ idbroker.knox_data_root }}/security/keystores/signing.jks
    - output_loglevel: debug

{{ idbroker.knox_data_root }}/security/keystores/signing.jks:
  file.managed:
    - mode: 644
    - replace: False
{% endif %}