add-cloudera-repo:
  pkgrepo.managed:
    - humanname: Cloudera
    - baseurl: https://archive.cloudera.com/cm6/6.1.0/redhat7/yum/
    - name: cloudera-manager
    - gpgcheck: 0
