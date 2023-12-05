{% if grains['os_family'] == 'RedHat' and grains['osmajorrelease'] | int == 8 and salt['pillar.get']('cluster:gov_cloud', False) == False %}
{% set repo_file = 'archive.cloudera.com/p/repos/rhel/server/' + salt['grains.get']('osmajorrelease') | string + '/' + salt['grains.get']('osrelease') | string + '/' + salt['grains.get']('osarch') + '/cloudera-repo/cloudera-repo.repo' %}

/etc/yum.repos.d/cloudera-repo.repo:
  file.managed:
{% if salt['pillar.get']('cloudera-manager:paywall_username') %}
    - source: https://{{ salt['pillar.get']('cloudera-manager:paywall_username') }}:{{ salt['pillar.get']('cloudera-manager:paywall_password') }}@{{ repo_file }}
{% else %}
    - source: https://{{ repo_file }}
{% endif %}
    - skip_verify: True
    - mode: 640
{% endif %}