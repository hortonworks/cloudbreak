{% if grains['os_family'] == 'RedHat' and grains['osmajorrelease'] | int >= 8 and salt['pillar.get']('cluster:gov_cloud', False) == False %}
/etc/yum.repos.d/cloudera-repo.repo:
  file.managed:
    - source: salt://rhelrepo/cloudera-repo.repo
    - template: jinja
    - mode: 640
{% endif %}