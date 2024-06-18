{% if salt['pillar.get']('idbroker:mastersecret', None) != None %}
{%- from 'idbroker/settings.sls' import idbroker with context %}
{%- set knoxUser = 'knox' %}
{%- set knoxGroup = 'knox' %}
{%- set mode = 640 if salt['pillar.get']('cluster:secretEncryptionEnabled', False) == True else 644 %}
{%- set useKnoxUser = True if salt['pillar.get']('cluster:secretEncryptionEnabled', False) == True else False %}
{%- set secretRoot = idbroker.knox_idbroker_secret_root if salt['pillar.get']('cluster:secretEncryptionEnabled', False) == True
else idbroker.knox_data_root ~ '/security' %}

{{ secretRoot }}/keystores/signkey.pem:
  file.managed:
    - contents_pillar: idbroker:signkey
    - makedirs: True
    - replace: True
{% if useKnoxUser == True %}
    - user: {{ knoxUser }}
    - group: {{ knoxGroup }}
{% endif %}
    - mode: {{ mode }}

{{ secretRoot }}/keystores/signcert.pem:
  file.managed:
    - contents_pillar: idbroker:signcert
    - makedirs: True
    - replace: True
{% if useKnoxUser == True %}
    - user: {{ knoxUser }}
    - group: {{ knoxGroup }}
{% endif %}
    - mode: {{ mode }}

# openssl pkcs12 -export -in cert.pem -inkey key.pem -out signing.p12 -name signing-identity -password pass:admin
# keytool -importkeystore -deststorepass admin1 -destkeypass admin1 -destkeystore signing.jks -srckeystore signing.p12 -srcstoretype PKCS12 -srcstorepass admin -alias signing-identity

idbroker-create-sign-pkcs12:
  cmd.run:
    - name: cd {{ secretRoot }}/keystores/ && openssl pkcs12 -export -in signcert.pem -inkey signkey.pem -out signing.p12 -name signing-identity -password pass:{{ salt['pillar.get']('idbroker:mastersecret') }}
    - creates: {{ secretRoot }}/keystores/signing.p12
    - output_loglevel: debug

{{ secretRoot }}/keystores/signing.p12:
  file.managed:
    - replace: True
{% if useKnoxUser == True %}
    - user: {{ knoxUser }}
    - group: {{ knoxGroup }}
{% endif %}
    - mode: {{ mode }}

idbroker-create-sign-{{ idbroker.keystore_type }}:
  cmd.run:
    - name: cd {{ secretRoot }}/keystores/ && keytool -importkeystore -deststorepass {{ salt['pillar.get']('idbroker:mastersecret') }} -destkeypass {{ salt['pillar.get']('idbroker:mastersecret') }} -destkeystore signing.{{ idbroker.keystore_type }} -deststoretype {{ idbroker.keystore_type | upper }} -srckeystore signing.p12 -srcstoretype PKCS12 -srcstorepass {{ salt['pillar.get']('idbroker:mastersecret') }} -alias signing-identity
    - creates: {{ secretRoot }}/keystores/signing.{{ idbroker.keystore_type }}
    - output_loglevel: debug

{{ secretRoot }}/keystores/signing.{{ idbroker.keystore_type }}:
  file.managed:
    - replace: True
{% if useKnoxUser == True %}
    - user: {{ knoxUser }}
    - group: {{ knoxGroup }}
{% endif %}
    - mode: {{ mode }}
{% endif %}