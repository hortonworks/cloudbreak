{%- from 'kerberos/settings.sls' import kerberos with context %}

include:
  - {{ slspath }}.common

create_krb5_conf_initialized:
  cmd.run:
    - name: touch /var/krb5-conf-initialized
    - shell: /bin/bash
    - unless: test -f /var/krb5-conf-initialized