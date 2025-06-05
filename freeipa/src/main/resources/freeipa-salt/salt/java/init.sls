{%- set tlsVersions = salt['pillar.get']('freeipa:tlsVersionsCommaSeparated') %}
{% set java_home = salt['environ.get']('JAVA_HOME') %}
{% if not java_home %}
  {% set java_home = '/usr/lib/jvm' %}
{% endif %}
