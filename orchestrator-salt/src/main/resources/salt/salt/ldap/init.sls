/usr/hdp/current/knox-server/conf/topologies/cloud.xml:
  file.managed:
    - source: salt://ldap/config/cloud.xml
    - template: jinja

su -l knox -c "/usr/hdp/current/knox-server/bin/ldap.sh start":
  cmd.run:
    - onlyif: 'test -e /usr/hdp/current/knox-server/bin/ldap.sh'