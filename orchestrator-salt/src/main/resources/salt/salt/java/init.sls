
#java-1.7.0-openjdk-devel:
#  pkg.installed: []
{% set java_home = salt['environ.get']('JAVA_HOME') %}
{% if not java_home %}
  {% set java_home = '/usr/lib/jvm/java' %}
{% endif %}


set_dns_ttl:
  file.replace:
    - name: {{ java_home }}/jre/lib/security/java.security
    - pattern: "#?networkaddress.cache.ttl=.*"
    - repl: "networkaddress.cache.ttl=5"
    - unless: cat {{ java_home }}/jre/lib/security/java.security | grep ^networkaddress.cache.ttl=5$

set_dns_negativ_ttl:
  file.replace:
    - name: {{ java_home }}/jre/lib/security/java.security
    - pattern: "#?networkaddress.cache.negative.ttl=.*"
    - repl: "networkaddress.cache.negative.ttl=0"
    - unless: cat {{ java_home }}/jre/lib/security/java.security | grep ^networkaddress.cache.negative.ttl=0$
