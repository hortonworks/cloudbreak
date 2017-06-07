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
