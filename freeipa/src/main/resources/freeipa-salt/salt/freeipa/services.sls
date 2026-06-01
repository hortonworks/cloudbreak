{%- if grains.get('saltversion', "") != "2017.7.5" %}

ipadnskeysyncdRestart:
   file.replace:
    - name: /usr/lib/systemd/system/ipa-dnskeysyncd.service
    - pattern: '^Restart=.*'
    - repl: 'Restart=always'
    - backup: False

ipadnskeysyncdRestartSec:
   file.replace:
     - name: /usr/lib/systemd/system/ipa-dnskeysyncd.service
     - pattern: '^RestartSec=.*'
     - repl: 'RestartSec=3'
     - backup: False

ipacustodiaRestart:
   file.replace:
    - name: /usr/lib/systemd/system/ipa-custodia.service
    - pattern: '^Restart=.*'
    - repl: 'Restart=always'
    - backup: False

ipacustodiaRestartSec:
   file.replace:
     - name: /usr/lib/systemd/system/ipa-custodia.service
     - pattern: '^RestartSec=.*'
     - repl: 'RestartSec=3'
     - backup: False

certmongerIpaServiceDependency:
   file.replace:
     - name: /usr/lib/systemd/system/certmonger.service
     - pattern: '^After=(.*)'
     - repl: 'After=\1 ipa.service'
     - backup: False
     - unless: grep "After=.*ipa\.service.*" /usr/lib/systemd/system/certmonger.service

{%- set enroll_ttls = salt['pillar.get']('freeipa:certmonger:enroll_ttls', '') %}
certmonger-enroll-ttls:
{%- if enroll_ttls != '' %}
  ini.options_present:
    - name: /etc/certmonger/certmonger.conf
    - separator: ' = '
    - sections:
        defaults:
          enroll_ttls: '{{ enroll_ttls }}'
{%- else %}
  ini.options_absent:
    - name: /etc/certmonger/certmonger.conf
    - sections:
        defaults:
          - enroll_ttls
    - onlyif: test -f /etc/certmonger/certmonger.conf
{%- endif %}

{%- if grains['init'] == 'systemd' %}
{%- for service in pillar['freeipa']['services'] %}
{%- set command = 'systemctl show -p FragmentPath ' + service %}
{%- set unitFile = salt['cmd.run'](command)  %}

{{ service }}Restart:
   file.line:
     - name: {{ unitFile | replace("FragmentPath=","") }}
     - mode: ensure
     - content: "Restart=always"
     - after: ^\[Service\]
     - backup: False

{{ service }}RestartSec:
   file.line:
     - name: {{ unitFile | replace("FragmentPath=","") }}
     - mode: ensure
     - content: "RestartSec=3"
     - after: "Restart=always"
     - backup: False

{%- endfor %}

{%- set domain = pillar['freeipa']['domain'] | upper | replace(".", "-") %}
{%- set command = 'systemctl show -p FragmentPath dirsrv@' + domain %}
{%- set unitFile = salt['cmd.run'](command)  %}

dirSrvRestart:
   file.line:
     - name: {{ unitFile | replace("FragmentPath=","") }}
     - mode: ensure
     - content: "Restart=always"
     - after: \[Service\]
     - backup: False

dirSrvRestartSec:
   file.line:
     - name: {{ unitFile | replace("FragmentPath=","") }}
     - mode: ensure
     - content: "RestartSec=3"
     - after: "Restart=always"
     - backup: False

reload-systemd:
  cmd.run:
    - name: systemctl daemon-reload
    - failhard: True

restart-certmonger:
  service.running:
    - name: certmonger
    - watch:
      - ini: certmonger-enroll-ttls

{%- endif %}

{%- endif %}