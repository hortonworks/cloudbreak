/etc/yum.repos.d/pgdg17.repo:
  file.managed:
{%- if grains['os_family'] == 'RedHat' and grains['osmajorrelease'] | int >= 8 %}
    - source: salt://postgresql/repo/postgres17-el8.repo
{%- endif %}
    - template: jinja
    - mode: 640

/etc/pki/rpm-gpg/RPM-GPG-KEY-PGDG-17:
  file.managed:
    - source: salt://postgresql/repo/pgdg17-gpg