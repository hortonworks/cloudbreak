{% set os = salt['grains.get']('os') %}
{% set osmajorrelease = salt['grains.get']('osmajorrelease') %}
{% if grains['os_family'] == 'RedHat' %}

/etc/yum.repos.d/clustermanager.repo:
  file.managed:
    - source: salt://cloudera/repo/clustermanager.repo
    - template: jinja

{% elif grains['os_family'] == 'Debian' %}

/etc/apt/sources.list.d/cloudera-manager.list:
  file.managed:
    - source: salt://cloudera/repo/cloudera-manager.list
    - template: jinja
{% if os == "Ubuntu"  and  osmajorrelease | int == 18 %}
  importKey:
    cmd.run:
      - name: wget -qO - {{ salt['pillar.get']('cloudera-manager:repo:baseUrl') }}/archive.key | sudo apt-key add -
{% endif %}

{% elif grains['os_family'] == 'Suse' %}

/etc/zypp/repos.d/cloudera-manager.repo:
  file.managed:
    - source: salt://cloudera/repo/clustermanager.repo
    - template: jinja

{% endif %}