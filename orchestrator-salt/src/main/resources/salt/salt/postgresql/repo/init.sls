{% if grains['os_family'] == 'RedHat' %}

/etc/yum.repos.d/pgdg10.repo:
  file.managed:
    - source: salt://postgresql/repo/pgdg10.repo
    - template: jinja

{% elif grains['os_family'] == 'Debian' %}

# No support for Debian

{% elif grains['os_family'] == 'Suse' %}

# No support for Suse

{% endif %}