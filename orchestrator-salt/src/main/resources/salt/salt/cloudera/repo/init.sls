{% if grains['os_family'] == 'RedHat' %}

/etc/yum.repos.d/cloudera-manager.repo:
  file.managed:
    - source: salt://cloudera/repo/cloudera-manager.repo
    - template: jinja

{% elif grains['os_family'] == 'Debian' %}

/etc/apt/sources.list.d/cloudera-manager.list:
  file.managed:
    - source: salt://cloudera/repo/cloudera-manager.list
    - template: jinja

{% elif grains['os_family'] == 'Suse' %}

/etc/zypp/repos.d/cloudera-manager.repo:
  file.managed:
    - source: salt://cloudera/repo/cloudera-manager.repo
    - template: jinja

{% endif %}