{%- set postgres_version = salt['pillar.get']('postgres:postgres_version', '11') | int %}
{%- do salt.log.debug("postgres_version " ~ postgres_version) %}

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
