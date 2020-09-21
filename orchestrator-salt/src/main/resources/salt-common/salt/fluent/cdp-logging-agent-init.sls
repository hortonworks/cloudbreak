{%- from 'fluent/settings.sls' import fluent with context %}
{% set os = salt['grains.get']('os') %}
{% set dbus_lock_exists = salt['file.file_exists' ]('/etc/cdp-logging-agent/databus_bundle.lock') %}

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
  file.managed:
    - source: salt://fluent/template/input.conf.j2
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
  file.managed:
    - source: salt://fluent/template/input.conf.j2
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

{%- if (not dbus_lock_exists) and fluent.dbusClusterLogsCollection %}
/etc/cdp-logging-agent/databus_bundle.lock:
   file.managed:
     - user: "{{ fluent.user }}"
     - group: "{{ fluent.group }}"
     - mode: '0640'
{% endif %}

{%- if fluent.is_systemd %}
/etc/systemd/system/cdp-logging-agent.d:
  file.directory:
    - name: /etc/systemd/system/cdp-logging-agent.d
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 740

/etc/systemd/system/cdp-logging-agent.d/override.conf:
   file.managed:
    - source: salt://fluent/template/override.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - mode: 640

fluentd_systemd_reload_and_run:
  file.copy:
    - name: /etc/systemd/system/cdp-logging-agent.service
    - source: /lib/systemd/system/cdp-logging-agent.service
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
      {%- if not fluent.dbusClusterLogsCollectionDisableStop %}
      - CDP_LOGGING_AGENT_POST_START_SCRIPT: /etc/cdp-logging-agent/post-start.sh
      - CDP_LOGGING_AGENT_POST_START_SCRIPT_PARAMS: 1200{% endif %}
{% endif %}

{%- if not fluent.dbusClusterLogsCollectionDisableStop %}
fluentd_write_simple_config_delayed:
   cmd.run:
    - name: "nohup sleep 30; cp /etc/cdp-logging-agent/cdp-logging-agent_simple_profile.conf /etc/cdp-logging-agent/cdp-logging-agent.conf &"
    - onlyif: "test -f /etc/cdp-logging-agent/cdp-logging-agent.conf && grep -q 'CLUSTER BUNDLE LOGS ENABLED' /etc/cdp-logging-agent/cdp-logging-agentt.conf"
{% endif %}
