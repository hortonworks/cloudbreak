{%- from 'fluent/settings.sls' import fluent with context %}
{% set os = salt['grains.get']('os') %}
{% set osmajorrelease = salt['grains.get']('osmajorrelease') %}
{% set dbus_lock_exists = salt['file.file_exists' ]('/etc/td-agent/databus_bundle.lock') %}

{% if fluent.enabled %}

{% if not salt['file.directory_exists' ]('/etc/td-agent') %}
{% if os == "RedHat" or os == "CentOS" %}
install_fluentd_yum:
  cmd.run:
    - name: curl -L https://toolbelt.treasuredata.com/sh/install-redhat-td-agent3.sh | sh
{% elif os == "Ubuntu" %}
  {% if osmajorrelease | int == 18 %}
install_fluentd_ubuntu18:
  cmd.run:
    - name: curl -L https://toolbelt.treasuredata.com/sh/install-ubuntu-bionic-td-agent3.sh | sh
  {% elif osmajorrelease | int == 16 %}
install_fluentd_ubuntu16:
  cmd.run:
    - name: curl -L https://toolbelt.treasuredata.com/sh/install-ubuntu-xenial-td-agent3.sh | sh
  {% elif osmajorrelease | int == 14 %}
install_fluentd_ubuntu14:
  cmd.run:
    - name: curl -L https://toolbelt.treasuredata.com/sh/install-ubuntu-trusty-td-agent3.sh | sh
  {% else %}
warning_fluentd_ubuntu:
  cmd.run:
    - name: echo "Warning - Fluentd install is not supported for this Ubuntu OS version ({{ os }})"
  {% endif %}
{% elif os == "Debian" %}
  {% if osmajorrelease | int == 9 %}
install_fluentd_debian9:
  cmd.run:
    - name: curl -L https://toolbelt.treasuredata.com/sh/install-debian-stretch-td-agent3.sh | sh
  {% elif osmajorrelease | int == 8 %}
install_fluentd_debian8:
  cmd.run:
    - name: curl -L https://toolbelt.treasuredata.com/sh/install-debian-jessie-td-agent3.sh | sh
  {% else %}
warning_fluentd_debian:
  cmd.run:
    - name: echo "Warning - Fluentd install is not supported for this Debian OS version ({{ os }})"
  {% endif %}
{% elif os == "SLES" %}
warning_fluentd_suse:
  cmd.run:
    - name: echo "Warning - Fluentd install is not supported yet for Suse ({{ os }})"
{% elif os == "Amazon" %}
{% if osmajorrelease | int == 2 %}
install_fluentd_amazon2:
  cmd.run:
    - name: curl -L https://toolbelt.treasuredata.com/sh/install-amazon2-td-agent3.sh | sh
{% elif osmajorrelease | int == 1 %}
install_fluentd_amazon1:
  cmd.run:
    - name: curl -L https://toolbelt.treasuredata.com/sh/install-amazon1-td-agent3.sh | sh
{% endif %}
{% else %}
warning_fluentd_os:
  cmd.run:
    - name: echo "Warning - Fluentd install is not supported for this OS type ({{ os }})"
{% endif %}
install_fluentd_plugins:
  cmd.run:
    - names:
      - /opt/td-agent/embedded/bin/fluent-gem source -a {{ fluent.clouderaPublicGemRepo }}
      - /opt/td-agent/embedded/bin/fluent-gem install fluent-plugin-cloudwatch-logs fluent-plugin-detect-exceptions
      - /opt/td-agent/embedded/bin/fluent-gem install fluent-plugin-databus -v {{ fluent.clouderaDatabusPluginVersion }}
      {% if fluent.platform == 'AZURE' %}
      - /opt/td-agent/embedded/bin/fluent-gem install fluent-plugin-azurestorage -v {{ fluent.clouderaAzurePluginVersion }} -s {{ fluent.clouderaPublicGemRepo }}
      - /opt/td-agent/embedded/bin/fluent-gem install fluent-plugin-azurestorage-gen2 -v {{ fluent.clouderaAzureGen2PluginVersion }}
      {% endif %}
    - onlyif: test -d /opt/td-agent/embedded/bin/
{% endif %}

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

/etc/td-agent/check_fluent_plugins.sh:
   file.managed:
    - source: salt://fluent/template/check_fluent_plugins.sh.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - file_mode: 750

check_fluentd_plugins:
   cmd.run:
    - name: sh /etc/td-agent/check_fluent_plugins.sh

{%- if fluent.is_systemd %}
fluent_systemd_stop:
  service.dead:
    - enable: False
    - name: td-agent
    - onlyif: "test -f /etc/td-agent/td-agent.conf && ! grep -q 'CONFIGURED BY SALT' /etc/td-agent/td-agent.conf"
{% else %}
fluent_stop:
  cmd.run:
    - name: "/etc/init.d/td-agent stop"
    - onlyif: "test -f /etc/td-agent/td-agent.conf && ! grep -q 'CONFIGURED BY SALT' /etc/td-agent/td-agent.conf"
{% endif %}

/etc/td-agent/td-agent_bundle_profile.conf:
  file.managed:
    - source: salt://fluent/template/td-agent.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - file_mode: 640
    - context:
        databusReportDeploymentLogs: "true"
        numberOfWorkers: {{ fluent.numberOfWorkers }}

/etc/td-agent/td-agent_simple_profile.conf:
  file.managed:
    - source: salt://fluent/template/td-agent.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - file_mode: 640
    - context:
        databusReportDeploymentLogs: "false"
{%- if fluent.dbusReportDeploymentLogs and (fluent.numberOfWorkers > 1) %}
        numberOfWorkers: {{ fluent.numberOfWorkers - 1 }}
{% else %}
        numberOfWorkers: {{ fluent.numberOfWorkers }}
{% endif %}

copy_td_agent_conf:
  cmd.run:
{%- if not fluent.dbusReportDeploymentLogs or (dbus_lock_exists and (not fluent.dbusReportDeploymentLogsDisableStop)) %}
    - name: "cp /etc/td-agent/td-agent_simple_profile.conf /etc/td-agent/td-agent.conf"
    - onlyif: "! diff /etc/td-agent/td-agent_simple_profile.conf /etc/td-agent/td-agent.conf"
{% else %}
    - name: "cp /etc/td-agent/td-agent_bundle_profile.conf /etc/td-agent/td-agent.conf"
    - onlyif: "! diff /etc/td-agent/td-agent_bundle_profile.conf /etc/td-agent/td-agent.conf"
{% endif %}

{% if fluent.cloudStorageLoggingEnabled or fluent.cloudLoggingServiceEnabled %}
/etc/td-agent/input.conf:
  file.managed:
    - source: salt://fluent/template/input.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - file_mode: 640
    - context:
        providerPrefix: {{ fluent.providerPrefix }}
        workerIndex: {{ fluent.cloudStorageWorkerIndex }}
{% endif %}

/etc/td-agent/databus_metering.conf:
   file.managed:
    - source: salt://fluent/template/databus_metering.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - file_mode: 640

/etc/td-agent/input_databus.conf:
  file.managed:
    - source: salt://fluent/template/input.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - file_mode: 640
    - context:
        providerPrefix: "databus"
        workerIndex: {{ fluent.reportDeploymentLogsWorkerIndex }}

/etc/td-agent/filter_databus.conf:
  file.managed:
    - name: /etc/td-agent/filter_databus.conf
    - source: salt://fluent/template/filter.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - file_mode: 640
    - context:
        providerPrefix: "databus"
        workerIndex: {{ fluent.reportDeploymentLogsWorkerIndex }}

/etc/td-agent/output_databus.conf:
  file.managed:
    - source: salt://fluent/template/output_databus.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - file_mode: 640

{% if fluent.cloudLoggingServiceEnabled %}
/etc/td-agent/filter.conf:
  file.managed:
    - name: /etc/td-agent/filter.conf
    - source: salt://fluent/template/filter.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - file_mode: 640
    - context:
        providerPrefix: {{ fluent.providerPrefix }}
        workerIndex: {{ fluent.cloudStorageWorkerIndex }}
{% endif %}

{% if fluent.cloudStorageLoggingEnabled or fluent.cloudLoggingServiceEnabled %}
/etc/td-agent/output.conf:
  file.managed:
    - name: /etc/td-agent/output.conf
    - source: salt://fluent/template/output.conf.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - file_mode: 640
{% endif %}

/etc/td-agent/databus_credential:
   file.managed:
    - source: salt://databus/template/databus_credential.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - file_mode: '0600'
    - context:
        profileName: "dbus"
        accessKeyIdName: "databus_access_key_id"
        accessKeySecretName: "databus_access_secret_key"
        accessKeySecretAlgoName: "databus_access_secret_key_algo"

{%- if (not dbus_lock_exists) and fluent.dbusReportDeploymentLogs %}
/etc/td-agent/databus_bundle.lock:
   file.managed:
     - user: "{{ fluent.user }}"
     - group: "{{ fluent.group }}"
     - file_mode: '0640'
{% endif %}

{%- if fluent.is_systemd %}
fluentd_start_with_update_systemd_units:
  file.copy:
    - name: /etc/systemd/system/td-agent.service
    - source: /lib/systemd/system/td-agent.service
  cmd.run:
    - names: 
      - "sed -i \"/User=/ s/=.*/={{ fluent.user }}/\" /etc/systemd/system/td-agent.service"
      - "sed -i \"/Group=/ s/=.*/={{ fluent.group }}/\" /etc/systemd/system/td-agent.service"
    - onlyif: test -f /etc/systemd/system/td-agent.service
  module.wait:
    - name: service.systemctl_reload
    - watch:
      - file: /etc/systemd/system/td-agent.service
  service.running:
    - enable: True
    - name: td-agent
    - watch:
       - file: /etc/systemd/system/td-agent.service
{% else %}

fs.file-max:
  sysctl.present:
    - value: 100000

fluent_start:
  cmd.run:
    - name: "/etc/init.d/td-agent start"
    - env:
      - TD_AGENT_USER: "{{ fluent.user }}"
      - TD_AGENT_GROUP: "{{ fluent.group }}"
{% endif %}

{%- if not fluent.dbusReportDeploymentLogsDisableStop %}
/etc/td-agent/delayed_restart.sh:
   file.managed:
    - source: salt://fluent/template/delayed_restart.sh.j2
    - template: jinja
    - user: "{{ fluent.user }}"
    - group: "{{ fluent.group }}"
    - file_mode: 750
    - onlyif: "test -f /etc/td-agent/td-agent.conf && grep -q 'CLUSTER BUNDLE LOGS ENABLED' /etc/td-agent/td-agent.conf"

fluentd_delalyed_restart:
   cmd.run:
    - names:
       - "nohup sh /etc/td-agent/delayed_restart.sh > /etc/td-agent/delayed_restart.out 2>&1 &"
       - "nohup sleep 30; cp /etc/td-agent/td-agent_simple_profile.conf /etc/td-agent/td-agent.conf &"
    - onlyif: "test -f /etc/td-agent/td-agent.conf && grep -q 'CLUSTER BUNDLE LOGS ENABLED' /etc/td-agent/td-agent.conf"
{% endif %}

{% endif %}
