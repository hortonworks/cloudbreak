{%- from 'fluent/settings.sls' import fluent with context %}
{%- from 'telemetry/settings.sls' import telemetry with context %}
{% set os = salt['grains.get']('os') %}
{% set restart_sleep_time = 1200 %}
{% set cpuarch = salt['grains.get']('cpuarch') %}
{% if fluent.cloudStorageLoggingEnabled %}
{% if fluent.uninstallTdAgent %}
td_agent_stop:
  service.dead:
    - enable: False
    - name: td-agent
uninstall_td_agent:
  pkg.purged:
    - name: td-agent
{% endif %}
{% if not fluent.cdpLoggingAgentInstalled %}
{% if os == "RedHat" or os == "CentOS" %}
install_cdp_logging_agent:
  cmd.run:
    - name: rpm -i {{ fluent.cdpLoggingAgentRpm }}
{% else %}
install_cdp_logging_agent_warning:
  cmd.run:
    - name: echo "CDP logging agent cannot be installed to {{ os }}"
{% endif %}
{% endif %}

{% if telemetry.devTelemetrySupported %}
intstall-dev-cdp-logging-agent:
  pkg.installed:
    - name: cdp-logging-agent
    - version: '{{ fluent.cdpLoggingAgentDevVersion }}'
{% endif %}

/etc/cdp-logging-agent/pos:
  file.directory:
    - name: /etc/cdp-logging-agent/pos
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 740
    - recurse:
      - user
      - group
      - mode

{%- if fluent.is_systemd %}
fluent_systemd_stop:
  service.dead:
    - enable: False
    - name: cdp-logging-agent
    - onlyif: "test -f /etc/cdp-logging-agent/cdp-logging-agent.conf && ! grep -q 'CONFIGURED BY SALT' /etc/cdp-logging-agent/cdp-logging-agent.conf"
{% else %}
fluent_stop:
  cmd.run:
    - name: "/etc/init.d/cdp-logging-agent stop"
    - onlyif: "test -f /etc/cdp-logging-agent/cdp-logging-agent.conf && ! grep -q 'CONFIGURED BY SALT' /etc/cdp-logging-agent/cdp-logging-agent.conf"
{% endif %}

/etc/cdp-logging-agent/cdp-logging-agent_bundle_profile.conf:
  file.managed:
    - source: salt://fluent/template/logging-agent.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: '0640'
    - context:
        numberOfWorkers: {{ fluent.numberOfWorkers }}

copy_cdp_logging_agent_conf:
  cmd.run:
    - name: "cp /etc/cdp-logging-agent/cdp-logging-agent_bundle_profile.conf /etc/cdp-logging-agent/cdp-logging-agent.conf"
    - onlyif: "! diff /etc/cdp-logging-agent/cdp-logging-agent_bundle_profile.conf /etc/cdp-logging-agent/cdp-logging-agent.conf"

/etc/cdp-logging-agent/input.conf:
  file.managed:
    - source: salt://fluent/template/input_vm_logs.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 640
    - context:
        providerPrefix: {{ fluent.providerPrefix }}
        workerIndex: {{ fluent.cloudStorageWorkerIndex }}

/etc/cdp-logging-agent/output.conf:
  file.managed:
    - name: /etc/cdp-logging-agent/output.conf
    - source: salt://fluent/template/output.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 640

/etc/cdp-logging-agent/databus_credential:
   file.managed:
    - source: salt://databus/template/databus_credential.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: '0600'
    - context:
        profileName: "dbus"
        accessKeyIdName: "databus_access_key_id"
        accessKeySecretName: "databus_access_secret_key"
        accessKeySecretAlgoName: "databus_access_secret_key_algo"

{%- if fluent.is_systemd %}
/etc/systemd/system/cdp-logging-agent.d:
  file.directory:
    - name: /etc/systemd/system/cdp-logging-agent.d
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 740

fluentd_systemd_reload_and_run:
  file.copy:
    - name: /etc/systemd/system/cdp-logging-agent.service
    - source: /lib/systemd/system/cdp-logging-agent.service
    - mode: 0644
  module.wait:
    - name: service.systemctl_reload
    - watch:
      - file: /etc/systemd/system/cdp-logging-agent.service
      - file: /etc/cdp-logging-agent/input.conf
      - file: /etc/cdp-logging-agent/output.conf

  service.running:
    - enable: True
    - name: cdp-logging-agent
    - watch:
       - file: /etc/systemd/system/cdp-logging-agent.service
       - file: /etc/cdp-logging-agent/input.conf
       - file: /etc/cdp-logging-agent/output.conf
{% else %}

fs.file-max:
  sysctl.present:
    - value: 100000

fluent_start:
  cmd.run:
    - name: "/etc/init.d/cdp-logging-agent start"
    - env:
      - CDP_LOGGING_AGENT_USER: "{{ fluent.user }}"
      - CDP_LOGGING_AGENT_GROUP: "{{ fluent.group }}"
{% endif %}

include:
  - fluent.crontab

{% endif %}
