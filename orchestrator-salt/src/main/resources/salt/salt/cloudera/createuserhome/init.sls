{% if "ipa_member" in grains.get('roles', []) and "namenode" in grains.get('roles', []) %}
/opt/salt/scripts/createuserhome.sh:
  file.managed:
    - source:
        - salt://cloudera/scripts/createuserhome.sh
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

