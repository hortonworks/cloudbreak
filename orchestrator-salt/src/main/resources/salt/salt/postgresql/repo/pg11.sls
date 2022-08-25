/etc/yum.repos.d/pgdg11.repo:
  file.managed:
    - source: salt://postgresql/repo/postgres11-el7.repo
    - template: jinja
    - mode: 640

/etc/pki/rpm-gpg/RPM-GPG-KEY-PGDG-11:
  file.managed:
    - source: salt://postgresql/repo/pgdg11-gpg