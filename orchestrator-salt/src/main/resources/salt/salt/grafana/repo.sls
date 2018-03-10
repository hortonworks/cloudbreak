{% if grains['os_family'] == 'RedHat' %}

/etc/yum.repos.d/grafana.repo:
  file.managed:
    - source: salt://grafana/yum/grafana.repo
    - template: jinja

{% elif grains['os_family'] == 'Debian' %}

create_grafana_repo:
  pkgrepo.managed:
    - name: "deb {{ salt['pillar.get']('grafana:repo:baseUrl') }} {{ salt['grains.get']('oscodename') }} main"
    - file: /etc/apt/sources.list.d/grafana.list
    - key_url: {{ salt['pillar.get']('grafana:repo:gpgKeyUrl') }}

{% endif %}
