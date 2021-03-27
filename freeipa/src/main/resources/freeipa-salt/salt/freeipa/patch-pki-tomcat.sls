restart_ipa:
  service.running:
    - name: pki-tomcatd@pki-tomcat.service
    - watch:
      - file: /var/lib/pki/pki-tomcat/conf/server.xml

restrict_pki_tomcat_8080_connector:
  file.replace:
    - name: /var/lib/pki/pki-tomcat/conf/server.xml
    - pattern: '(^.*<Connector name="Unsecure" port="8080") (.*$)'
    - repl: '\1 address="localhost" \2'

restrict_pki_tomcat_8443_connector:
  file.replace:
    - name: /var/lib/pki/pki-tomcat/conf/server.xml
    - pattern: '(^.*<Connector name="Secure" port="8443") (.*$)'
    - repl: '\1 address="localhost" \2'
