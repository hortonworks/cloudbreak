/opt/salt/scripts/set_default_java_version.sh:
  file.managed:
    - source:
        - salt://java/scripts/set_default_java_version.sh
    - makedirs: True
    - mode: 755

set_default_java_version:
  cmd.run:
    - name: /opt/salt/scripts/set_default_java_version.sh
    - env:
        - TARGET_JAVA_VERSION: {{salt['pillar.get']('java:version')}}
    - require:
      - file: /opt/salt/scripts/set_default_java_version.sh

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
