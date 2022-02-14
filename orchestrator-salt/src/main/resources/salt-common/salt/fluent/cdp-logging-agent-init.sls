{%- from 'fluent/settings.sls' import fluent with context %}
{%- from 'telemetry/settings.sls' import telemetry with context %}
{% set os = salt['grains.get']('os') %}
{% set dbus_lock_exists = salt['file.file_exists' ]('/etc/cdp-logging-agent/databus_bundle.lock') %}
{% set restart_sleep_time = 1200 %}

/etc/cdp-logging-agent/post-start.sh:
  file.managed:
    - source: salt://fluent/template/post-start.sh.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: '0750'

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
        monitorFilesForDbusProcessing: "true"
        numberOfWorkers: {{ fluent.numberOfWorkers }}

/etc/cdp-logging-agent/cdp-logging-agent_simple_profile.conf:
  file.managed:
    - source: salt://fluent/template/logging-agent.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: '0640'
    - context:
        monitorFilesForDbusProcessing: "false"
        numberOfWorkers: {{ fluent.numberOfWorkers }}

copy_cdp_logging_agent_conf:
  cmd.run:
{%- if not fluent.dbusClusterLogsCollection or (dbus_lock_exists and (not fluent.dbusClusterLogsCollectionDisableStop)) %}
    - name: "cp /etc/cdp-logging-agent/cdp-logging-agent_simple_profile.conf /etc/cdp-logging-agent/cdp-logging-agent.conf"
    - onlyif: "! diff /etc/cdp-logging-agent/cdp-logging-agent_simple_profile.conf /etc/cdp-logging-agent/cdp-logging-agent.conf"
{% else %}
    - name: "cp /etc/cdp-logging-agent/cdp-logging-agent_bundle_profile.conf /etc/cdp-logging-agent/cdp-logging-agent.conf"
    - onlyif: "! diff /etc/cdp-logging-agent/cdp-logging-agent_bundle_profile.conf /etc/cdp-logging-agent/cdp-logging-agent.conf"
{% endif %}

{% if fluent.cloudStorageLoggingEnabled or fluent.cloudLoggingServiceEnabled %}
/etc/cdp-logging-agent/input.conf:
  file.managed:{% if telemetry.logs %}
    - source: salt://fluent/template/input_vm_logs.conf.j2{% else %}
    - source: salt://fluent/template/input.conf.j2{% endif %}
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 640
    - context:
        providerPrefix: {{ fluent.providerPrefix }}
        workerIndex: {{ fluent.cloudStorageWorkerIndex }}
{% endif %}

/etc/cdp-logging-agent/databus_metering.conf:
   file.managed:
    - source: salt://fluent/template/databus_metering.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 640

/etc/cdp-logging-agent/databus_monitoring.conf:
   file.managed:
    - source: salt://fluent/template/databus_monitoring.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 640

/etc/cdp-logging-agent/input_databus.conf:
  file.managed:{% if telemetry.logs %}
    - source: salt://fluent/template/input_vm_logs.conf.j2{% else %}
    - source: salt://fluent/template/input.conf.j2{% endif %}
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 640
    - context:
        providerPrefix: "databus"
        workerIndex: {{ fluent.clusterLogsCollectionWorkerIndex }}

/etc/cdp-logging-agent/input_databus_stream.conf:
  file.managed:
    - source: salt://fluent/template/input_databus_stream.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 640
    - context:
        workerIndex: {{ fluent.clusterLogsCollectionWorkerIndex }}

/etc/cdp-logging-agent/filter_databus.conf:
  file.managed:
    - name: /etc/cdp-logging-agent/filter_databus.conf
    - source: salt://fluent/template/filter.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 640
    - context:
        providerPrefix: "databus"
        workerIndex: {{ fluent.clusterLogsCollectionWorkerIndex }}

/etc/cdp-logging-agent/output_databus.conf:
  file.managed:
    - source: salt://fluent/template/output_databus.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 640

{% if fluent.cloudLoggingServiceEnabled %}
/etc/cdp-logging-agent/filter.conf:
  file.managed:
    - name: /etc/cdp-logging-agent/filter.conf
    - source: salt://fluent/template/filter.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 640
    - context:
        providerPrefix: {{ fluent.providerPrefix }}
        workerIndex: {{ fluent.cloudStorageWorkerIndex }}
{% endif %}

{% if fluent.cloudStorageLoggingEnabled or fluent.cloudLoggingServiceEnabled %}
/etc/cdp-logging-agent/output.conf:
  file.managed:
    - name: /etc/cdp-logging-agent/output.conf
    - source: salt://fluent/template/output.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 640
{% endif %}

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
      - file: /etc/systemd/system/cdp-logging-agent.service{% if fluent.cloudStorageLoggingEnabled or fluent.cloudLoggingServiceEnabled %}
      - file: /etc/cdp-logging-agent/input.conf
      - file: /etc/cdp-logging-agent/output.conf{% endif %}
      - file: /etc/cdp-logging-agent/input_databus.conf
      - file: /etc/cdp-logging-agent/filter_databus.conf
      - file: /etc/cdp-logging-agent/databus_metering.conf

  service.running:
    - enable: True
    - name: cdp-logging-agent
    - watch:
       - file: /etc/systemd/system/cdp-logging-agent.service{% if fluent.cloudStorageLoggingEnabled or fluent.cloudLoggingServiceEnabled %}
       - file: /etc/cdp-logging-agent/input.conf
       - file: /etc/cdp-logging-agent/output.conf{% endif %}
       - file: /etc/cdp-logging-agent/input_databus.conf
       - file: /etc/cdp-logging-agent/filter_databus.conf
       - file: /etc/cdp-logging-agent/output_databus.conf
       - file: /etc/cdp-logging-agent/databus_metering.conf
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

{%- if not fluent.dbusClusterLogsCollectionDisableStop %}
fluentd_delalyed_restart:
   cmd.run:
    - names:
        - "nohup sleep 30; cp /etc/cdp-logging-agent/cdp-logging-agent_simple_profile.conf /etc/cdp-logging-agent/cdp-logging-agent.conf &"
        - "nohup sh /etc/cdp-logging-agent/post-start.sh {{ restart_sleep_time }} > /etc/cdp-logging-agent/delayed_restart.out 2>&1 &"
    - onlyif: "test -f /etc/cdp-logging-agent/cdp-logging-agent.conf && ! test -f /etc/cdp-logging-agent/databus_bundle.lock && grep -q 'CLUSTER BUNDLE LOGS ENABLED' /etc/cdp-logging-agent/cdp-logging-agent.conf"
{% endif %}
include:
  - fluent.crontab
