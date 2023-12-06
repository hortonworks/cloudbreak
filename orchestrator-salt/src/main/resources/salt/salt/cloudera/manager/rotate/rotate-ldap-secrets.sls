
rotate-ldap-bindpassword:
  file.replace:
    - name: /etc/cloudera-scm-server/cm.settings
    - pattern: "^setsettings LDAP_BIND_PW .*"
    - repl: "setsettings LDAP_BIND_PW {{ salt['pillar.get']('ldap:bindPassword') }}"
    - append_if_not_found: True
