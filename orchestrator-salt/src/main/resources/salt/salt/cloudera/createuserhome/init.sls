{% set roles = salt['grains.get']('roles') %}
{% if ("ipa_member" in roles or "ad_member" in roles) and "namenode" in roles %}

/opt/salt/scripts/createuserhome.sh:
  file.managed:
    - template: jinja
    - source:
        - salt://cloudera/scripts/createuserhome.sh
    - context:
        ldap: {{ salt['pillar.get']('ldap') }}
        hiveWithRemoteHiveMetastore: {{ salt['pillar.get']('cluster:hiveWithRemoteHiveMetastore', False) }}
    - makedirs: True
    - mode: 755

createusername-cron:
  cron.present:
    - name: /opt/salt/scripts/createuserhome.sh >> /var/log/createusername.log 2>&1
    - user: root
    - minute: '*/5'
    - require:
        - file: /opt/salt/scripts/createuserhome.sh
{% endif %}

