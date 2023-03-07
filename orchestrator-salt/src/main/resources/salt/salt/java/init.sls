/opt/salt/scripts/set_default_java_version.sh:
  file.managed:
    - source:
        - salt://java/scripts/set_default_java_version.sh
    - makedirs: True
    - mode: 755

set_default_java_version:
  cmd.run:
    - name: /opt/salt/scripts/set_default_java_version.sh 2>&1 | tee -a /var/log/set-default-java-version.log && [[ 0 -eq ${PIPESTATUS[0]} ]] && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/set-default-java-version-executed || exit ${PIPESTATUS[0]}
    - runas: root
    - env:
        - TARGET_JAVA_VERSION: {{salt['pillar.get']('java:version')}}
    - failhard: True
    - require:
      - file: /opt/salt/scripts/set_default_java_version.sh
    - unless: test -f /var/log/set-default-java-version-executed

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

{% if salt['pillar.get']('cluster:gov_cloud', False) == True %}

/opt/salt/scripts/install_safelogic_binaries.sh:
  file.managed:
    - source: salt://java/scripts/install_safelogic_binaries.sh
    - makedirs: True
    - mode: 700

install_safelogic_binaries:
  cmd.run:
    - name: /opt/salt/scripts/install_safelogic_binaries.sh 2>&1 | tee -a /var/log/install_safelogic_binaries.log && [[ 0 -eq ${PIPESTATUS[0]} ]] && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/safelogic_binaries_installed || exit ${PIPESTATUS[0]}
    - env:
        JRE_EXT_PATH: "{{ java_home }}/jre/lib/ext"
        PAYWALL_AUTH: "{{ salt['pillar.get']('cloudera-manager:paywall_username') }}:{{ salt['pillar.get']('cloudera-manager:paywall_password') }}"
        CCJ_PATH: "{{ salt['pillar.get']('java:safelogic:cryptoComplyPath') }}"
        CCJ_HASH_PATH: "{{ salt['pillar.get']('java:safelogic:cryptoComplyHash') }}"
        BCTLS_PATH: "{{ salt['pillar.get']('java:safelogic:bouncyCastleTlsPath') }}"
        BCTLS_HASH_PATH: "{{ salt['pillar.get']('java:safelogic:bouncyCastleTlsHash') }}"
    - require:
        - file: /opt/salt/scripts/install_safelogic_binaries.sh
    - failhard: True

{% endif %}
