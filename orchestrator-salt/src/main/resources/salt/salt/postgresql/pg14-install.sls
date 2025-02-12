{% if not salt['file.file_exists']('/usr/pgsql-14/bin/psql') %}
include:
  - postgresql.repo.pg14

install-postgres14:
  pkg.installed:
    - failhard: True
    - pkgs:
        - postgresql14-server
        - postgresql14
        - postgresql14-contrib
        - postgresql14-docs
    - fromrepo: pgdg14
{% endif %}