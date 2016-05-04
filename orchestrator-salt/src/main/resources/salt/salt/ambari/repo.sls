/etc/yum.repos.d/ambari.repo:
  file.managed:
    - source: salt://ambari/yum/ambari.repo
    - template: jinja
