add_ambari_proxy_configuration:
  file.append:
    - name: /var/lib/ambari-server/ambari-env.sh
    - template: jinja
    - source: salt://ambari/template/proxy.j2
    - unless: grep "proxyHost" /var/lib/ambari-server/ambari-env.sh

