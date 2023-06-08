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
        - postgresql14-devel
        - postgresql-jdbc
    - fromrepo: pgdg14