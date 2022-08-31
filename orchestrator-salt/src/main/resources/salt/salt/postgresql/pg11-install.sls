include:
  - postgresql.repo.pg11

install-postgres11:
  pkg.installed:
    - failhard: True
    - pkgs:
        - postgresql11-server
        - postgresql11
        - postgresql11-contrib
        - postgresql11-docs