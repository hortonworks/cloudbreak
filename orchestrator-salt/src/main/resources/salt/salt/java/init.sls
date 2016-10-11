
#java-1.7.0-openjdk-devel:
#  pkg.installed: []

set_dns_ttl:
  file.replace:
    - name: /usr/lib/jvm/java/jre/lib/security/java.security
    - pattern: "#?networkaddress.cache.ttl=.*"
    - repl: "networkaddress.cache.ttl=5"
    - unless: cat /usr/lib/jvm/java/jre/lib/security/java.security | grep ^networkaddress.cache.ttl=5$

set_dns_negativ_ttl:
  file.replace:
    - name: /usr/lib/jvm/java/jre/lib/security/java.security
    - pattern: "#?networkaddress.cache.negative.ttl=.*"
    - repl: "networkaddress.cache.negative.ttl=0"
    - unless: cat /usr/lib/jvm/java/jre/lib/security/java.security | grep ^networkaddress.cache.negative.ttl=0$
