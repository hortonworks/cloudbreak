{%- from 'fluent/settings.sls' import fluent with context %}
{% set os = salt['grains.get']('os') %}
{% set osmajorrelease = salt['grains.get']('osmajorrelease') %}

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
      - /opt/td-agent/embedded/bin/fluent-gem install fluent-plugin-databus fluent-plugin-cloudwatch-logs
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
fluent_start:
  cmd.run:
    - name: "/etc/init.d/td-agent start"
    - env:
      - TD_AGENT_USER: "{{ fluent.user }}"
      - TD_AGENT_GROUP: "{{ fluent.group }}"
{% endif %}
{% endif %}
