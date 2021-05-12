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

{%- if grains['init'] == 'systemd' %}
{%- for service in pillar['freeipa']['services'] %}
{%- set command = 'systemctl show -p FragmentPath ' + service %}
{%- set unitFile = salt['cmd.run'](command)  %}

{{ service }}Restart:
   file.line:
     - name: {{ unitFile | replace("FragmentPath=","") }}
     - mode: ensure
     - content: "Restart=always"
     - after: \[Service\]
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

{%- endif %}