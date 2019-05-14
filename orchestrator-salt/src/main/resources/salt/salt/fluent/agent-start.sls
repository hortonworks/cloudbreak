{%- from 'fluent/settings.sls' import fluent with context %}
{% if fluent.enabled %}
/etc/td-agent/pos:
  file.directory:
    - name: /etc/td-agent/pos
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 740
    - recurse:
      - user
      - group
      - mode

/etc/td-agent/td-agent.conf:
  file.managed:
    - source: salt://fluent/template/td-agent.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - file_mode: 640

/etc/td-agent/output.conf:
  file.managed:
    - source: salt://fluent/template/output.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - file_mode: 640
{%- if salt['cmd.retcode']('test -f /lib/systemd/system/td-agent.service') == 0 %}
update_systemd_units:
  cmd.run:
    - names:
      - "sed -i \"/User=/ s/=.*/={{ fluent.user }}/\" /lib/systemd/system/td-agent.service"
      - "sed -i \"/Group=/ s/=.*/={{ fluent.group }}/\" /lib/systemd/system/td-agent.service"
      - "systemctl daemon-reload"
{% endif %}
fluent_start:
  cmd.run:
    - name: "/etc/init.d/td-agent start"
    - env:
      - TD_AGENT_USER: "{{ fluent.user }}"
      - TD_AGENT_GROUP: "{{ fluent.group }}"
{% endif %}
