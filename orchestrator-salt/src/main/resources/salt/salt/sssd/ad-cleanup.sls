/opt/salt/scripts/remove_cm_principals.sh:
  file.managed:
    - source: salt://sssd/template/remove_cm_principals.j2
    - template: jinja
    - context:
        kerberos: {{ salt['pillar.get']('kerberos') }}
        ldap: {{ salt['pillar.get']('ldap') }}
        all_hostnames: {{ salt['pillar.get']('ad-cleanup-nodes:all_hostnames') }}
    - makedirs: True
    - mode: 700

remove-cm-principals:
  cmd.run:
    - name: /opt/salt/scripts/remove_cm_principals.sh
    - require:
        - file: /opt/salt/scripts/remove_cm_principals.sh