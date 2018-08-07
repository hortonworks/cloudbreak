{%- from 'ambari/settings.sls' import ambari with context %}

{% if not ambari.setup_ldap_and_sso_on_api %}
{% if ambari.gateway.ssotype is defined and ambari.gateway.ssotype is not none and ambari.gateway.ssotype == 'SSO_PROVIDER' %}

ambari_sso_enabled:
  file.append:
    - name: /etc/ambari-server/conf/ambari.properties
    - text: authentication.jwt.enabled=true
    - unless: grep "authentication.jwt.enabled" /etc/ambari-server/conf/ambari.properties
    - require:
      - pkg: ambari-server

ambari_sso_providerurl:
  file.append:
    - name: /etc/ambari-server/conf/ambari.properties
    - text: authentication.jwt.providerUrl=https://{{ ambari.gateway.address }}:8443{{ ambari.gateway.ssoprovider }}
    - unless: grep "authentication.jwt.providerUrl" /etc/ambari-server/conf/ambari.properties
    - require:
      - pkg: ambari-server

ambari_sso_publickey:
  file.append:
    - name: /etc/ambari-server/conf/ambari.properties
    - text: authentication.jwt.publicKey=/etc/ambari-server/conf/jwt-cert.pem
    - unless: grep "authentication.jwt.publicKey" /etc/ambari-server/conf/ambari.properties
    - require:
      - pkg: ambari-server

/etc/ambari-server/conf/jwt-cert.pem:
  file.managed:
    - contents_pillar: gateway:signcert
    - require:
      - pkg: ambari-server

{% endif %}
{% endif %}