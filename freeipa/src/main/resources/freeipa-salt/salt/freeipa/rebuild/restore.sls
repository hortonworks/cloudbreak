# it's expected to fail with pki-tomcatd not able to start
restore_full_backup:
  cmd.run:
    - name: ipa-restore -p $FPW --unattended /var/lib/ipa/restore/full 2>&1 | tee -a /var/log/ipa-restore-full.log
    - env:
        - FPW: {{salt['pillar.get']('freeipa:password')}}
    - failhard: True

clear_ipaserver_from_hosts:
  file.replace:
    - name: /etc/hosts
    - pattern: '.*ipaserver.*'
    - repl: ''
    - append_if_not_found: False
    - show_changes: True

{% for host in salt['pillar.get']('freeipa:hosts') %}
{{ host['ip'] }}:
  host.only:
    - hostnames:
      - {{ host['fqdn'] }}
{% if '.' in host['fqdn'] %}
      - {{ host['fqdn'].split('.')[0] }}
{% endif %}
{% endfor %}

restart_freeipa:
  cmd.run:
    - name: ipactl restart
    - failhard: True

restore_data_backup:
  cmd.run:
    - name: ipa-restore -p $FPW --unattended --online --data /var/lib/ipa/restore/data 2>&1 | tee -a /var/log/ipa-restore-data.log && exit ${PIPESTATUS[0]}
    - env:
        - FPW: {{salt['pillar.get']('freeipa:password')}}
    - failhard: True

/opt/salt/scripts/update_dns_record_rebuild.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 700
    - source: salt://freeipa/scripts/update_dns_record_rebuild.sh

update_dns_record:
  cmd.run:
    - name: /opt/salt/scripts/update_dns_record_rebuild.sh 2>&1 | tee -a /var/log/ipa-restore-update-dns.log && exit ${PIPESTATUS[0]}
    - failhard: True
    - env:
        - ADMIN_PASSWORD: {{salt['pillar.get']('freeipa:password')}}
    - require:
        - file: /opt/salt/scripts/update_dns_record_rebuild.sh
    - retry:
        attempts: 10
        interval: 30

stop_sssd:
  service.dead:
    - name: sssd

remove_sssd_cache_files:
  cmd.run:
    - name: |
        find /var/lib/sss/ ! -type d | xargs rm -f
