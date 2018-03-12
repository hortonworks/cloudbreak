{% if grains['os_family'] == 'RedHat' %}

/etc/yum.repos.d/HDP.repo:
  file.managed:
    - replace: False
    - source: salt://gateway/yum/hdp.repo
    - template: jinja

/etc/yum.repos.d/HDP-UTILS.repo:
  file.managed:
    - replace: False
    - source: salt://gateway/yum/hdp-utils.repo
    - template: jinja

{% if 'HDF' in salt['pillar.get']('hdp:stack:repoid') %}

/etc/yum.repos.d/KNOX.repo:
  file.managed:
    - replace: False
    - source: salt://gateway/yum/knox.repo
    - template: jinja

{% endif %}

{% elif grains['os_family'] == 'Debian' %}

create_hdp_repo:
  pkgrepo.managed:
    - name: "deb {{ salt['pillar.get']('hdp:stack:ubuntu16') }} HDP main"
    - file: /etc/apt/sources.list.d/hdp.list

create_hdp_utils_repo:
  pkgrepo.managed:
    - name: "deb {{ salt['pillar.get']('hdp:util:ubuntu16') }} HDP-UTILS main"
    - file: /etc/apt/sources.list.d/hdp-utils.list

{% if 'HDF' in salt['pillar.get']('hdp:stack:repoid') %}

create_knox_repo:
  pkgrepo.managed:
    - name: "deb {{ salt['pillar.get']('hdp:knox:ubuntu16') }} HDP main"
    - file: /etc/apt/sources.list.d/knox.list

{% endif %}

{% endif %}
