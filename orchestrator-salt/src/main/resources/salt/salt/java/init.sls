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
        - CPU_ARCH: {{salt['grains.get']('cpuarch')}}
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
    - unless: test -f /var/log/safelogic_binaries_installed

{% set java_policy_file = java_home ~ '/jre/lib/security/java.policy' %}
{% set java_security_file = java_home ~ '/jre/lib/security/java.security' %}

set_java_policy:
  file.managed:
    - name: {{ java_policy_file }}
    - source: salt://java/templates/java.policy
    - user: root
    - group: root
    - mode: 644

java_security_set_security_providers:
  file.blockreplace:
    - name: {{ java_security_file }}
    - marker_start: "# List of providers and their preference orders (see above):"
    - marker_end: "# Security providers used when FIPS mode support is active"
    - template: jinja
    - context:
        provider_type: "security"
    - source: salt://java/templates/java_security_providers.j2

java_security_set_fips_providers:
  file.blockreplace:
    - name: {{ java_security_file }}
    - marker_start: "# Security providers used when FIPS mode support is active"
    - marker_end: "# Sun Provider SecureRandom seed source."
    - template: jinja
    - context:
        provider_type: "fips"
    - source: salt://java/templates/java_security_providers.j2

java_security_set_keymanagerfactory_algorithm:
  file.replace:
    - name: {{ java_security_file }}
    - pattern: "^ssl.KeyManagerFactory.algorithm=.*"
    - repl: "ssl.KeyManagerFactory.algorithm=X.509"
    - append_if_not_found: True

{% else %}

{% if grains['os_family'] == 'RedHat' and grains['osmajorrelease'] | int >= 8 %}
change_krb5_conf_crypto_policies:
  file.managed:
    - name: /etc/krb5.conf.d/crypto-policies
    - replace: True
    - contents: |
        [libdefaults]
        permitted_enctypes = aes256-cts-hmac-sha1-96 aes256-cts-hmac-sha384-192 camellia256-cts-cmac aes128-cts-hmac-sha1-96 aes128-cts-hmac-sha256-128 camellia128-cts-cmac
{% endif %}

{% endif %}

{% if pillar.get('java:rootCertificates') is defined %}

/opt/salt/scripts/import_pdl_certs.sh:
  file.managed:
    - template: jinja
    - source: salt://java/scripts/import_pdl_certs.sh.j2
    - user: root
    - group: root
    - mode: 700

import_pdl_certs:
  cmd.run:
    - name: /opt/salt/scripts/import_pdl_certs.sh 2>&1 | tee -a /var/log/import_certs.log && exit ${PIPESTATUS[0]}
    - failhard: True
    - require:
        - file: /opt/salt/scripts/import_pdl_certs.sh
    - onchanges:
        - file: /opt/salt/scripts/import_pdl_certs.sh
{% endif %}
