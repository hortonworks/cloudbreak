restart_pki_tomcat:
  service.running:
      - name: pki-tomcatd@pki-tomcat.service
      - watch:
        - file: /var/lib/pki/pki-tomcat/conf/web.xml

replace_default_tomcat_error_page:
  file.replace:
    - name: /var/lib/pki/pki-tomcat/conf/web.xml
    - pattern: '</welcome-file-list>\s*</web-app>'
    - repl: '</welcome-file-list>\n
             <error-page>\n
             <error-code>404</error-code>\n
                <location>/dummy.jsp</location>\n
             </error-page>\n
             <error-page>\n
             <error-code>403</error-code>\n
                <location>/dummy.jsp</location>\n
             </error-page>\n
             <error-page>\n
             <error-code>500</error-code>\n
                <location>/dummy.jsp</location>\n
             </error-page>\n
             </web-app>'
    - backup: False