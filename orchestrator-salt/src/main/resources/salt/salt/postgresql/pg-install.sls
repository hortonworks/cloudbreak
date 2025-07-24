{% set postgres_version = salt['pillar.get']('postgres:postgres_version', '10') | int %}

include:
  - postgresql.repo.pg{{ postgres_version }}

install-postgres:
  pkg.installed:
    - failhard: True
    - pkgs:
        - postgresql{{ postgres_version }}-server
        - postgresql{{ postgres_version }}
        - postgresql{{ postgres_version }}-contrib
        - postgresql{{ postgres_version }}-docs
    - fromrepo: pgdg{{ postgres_version }}