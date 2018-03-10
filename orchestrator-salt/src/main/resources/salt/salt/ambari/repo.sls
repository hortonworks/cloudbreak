{% if grains['os_family'] == 'RedHat' %}

/etc/yum.repos.d/ambari.repo:
  file.managed:
    - source: salt://ambari/yum/ambari.repo
    - template: jinja

{% elif grains['os_family'] == 'Debian' %}

create_ambari_repo:
  pkgrepo.managed:
    - name: "deb {{ salt['pillar.get']('ambari:repo:baseUrl') }} Ambari main"
    - file: /etc/apt/sources.list.d/ambari.list
{% if salt['pillar.get']('ambari:repo:gpgKeyUrl') %}
    - key_url: "{{ salt['pillar.get']('ambari:repo:gpgKeyUrl') }}"
{% endif %}

{% endif %}