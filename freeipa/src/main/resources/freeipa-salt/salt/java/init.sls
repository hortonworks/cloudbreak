{%- set tlsv13Enabled = True if salt['pillar.get']('freeipa:tlsv13Enabled', False) == True else False %}
{% set java_home = salt['environ.get']('JAVA_HOME') %}
{% if not java_home %}
  {% set java_home = '/usr/lib/jvm' %}
{% endif %}
{% if tlsv13Enabled == True %}
set_tls_version:
  file.append:
    - name: {{ java_home }}/jre/lib/security/java.security
    - text:
        - jdk.tls.client.protocols=TLSv1.3
        - jdk.tls.server.protocols=TLSv1.3
{% endif %}
