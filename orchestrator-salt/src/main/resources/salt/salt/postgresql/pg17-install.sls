{% if not salt['file.file_exists']('/usr/pgsql-17/bin/psql') %}
include:
  - postgresql.repo.pg17

install-postgres17:
  pkg.installed:
    - failhard: True
    - pkgs:
        - postgresql17-server
        - postgresql17
        - postgresql17-contrib
        - postgresql17-docs
    - fromrepo: pgdg17
{% endif %}