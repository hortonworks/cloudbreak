include:
  - postgresql.repo.pg11

install-centos-scl-rh:
  pkg.installed:
    - failhard: True
    - name: centos-release-scl-rh

install-postgres11:
  pkg.installed:
    - failhard: True
    - pkgs:
        - postgresql11-server
        - postgresql-jdbc
        - postgresql11
        - postgresql11-contrib
        - postgresql11-docs
        - postgresql11-devel