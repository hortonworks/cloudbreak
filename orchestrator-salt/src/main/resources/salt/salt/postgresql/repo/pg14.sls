/etc/yum.repos.d/pgdg14.repo:
  file.managed:
{%- if grains['os_family'] == 'RedHat' and grains['osmajorrelease'] | int == 7 %}
    - source: salt://postgresql/repo/postgres14-el7.repo
{%- elif grains['os_family'] == 'RedHat' and grains['osmajorrelease'] | int >= 8 %}
    - source: salt://postgresql/repo/postgres14-el8.repo
{%- endif %}
    - template: jinja
    - mode: 640

/etc/pki/rpm-gpg/RPM-GPG-KEY-PGDG-14:
  file.managed:
    - source: salt://postgresql/repo/pgdg14-gpg