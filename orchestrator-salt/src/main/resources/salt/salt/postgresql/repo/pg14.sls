/etc/yum.repos.d/pgdg14.repo:
  file.managed:
    - source: salt://postgresql/repo/postgres14-el7.repo
    - template: jinja
    - mode: 640

/etc/pki/rpm-gpg/RPM-GPG-KEY-PGDG-14:
  file.managed:
    - source: salt://postgresql/repo/pgdg14-gpg