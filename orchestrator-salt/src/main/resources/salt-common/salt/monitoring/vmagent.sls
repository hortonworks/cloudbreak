{%- from 'telemetry/settings.sls' import telemetry with context %}
{%- from 'monitoring/settings.sls' import monitoring with context %}

{%- if monitoring.enabled %}
{% set vmagent_installed = salt['file.directory_exists' ]('/opt/cdp-vmagent') %}
{%- if not vmagent_installed %}
install_vmagent:
  cmd.run:
    - name: "yum install -y cdp-vmagent --disablerepo='*' --enablerepo=cdp-infra-tools; exit 0"
    - onlyif: "test -f /etc/yum.repos.d/cdp-infra-tools.repo && ! rpm -q cdp-vmagent"
{%- endif %}

/opt/cdp-vmagent/noproxy_check.py:
  file.managed:
    - source: salt://monitoring/scripts/noproxy_check.py
    - user: "root"
    - group: "root"
    - mode: 750

/etc/systemd/system/cdp-vmagent.service:
  file.managed:
    - source: salt://monitoring/systemd/cdp-vmagent.service.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 640

/opt/cdp-vmagent/prometheus.yml:
  file.managed:
    - source: salt://monitoring/template/prometheus.yml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 640

{%- if monitoring.password %}
/opt/cdp-vmagent/remote_pwd_file:
  file.managed:
    - source: salt://monitoring/template/remote_pwd_file.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600
{%- elif monitoring.token %}
/opt/cdp-vmagent/remote_token_file:
  file.managed:
    - source: salt://monitoring/template/remote_token_file.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600
{%- endif %}

start_cdp_vmagent:
  service.running:
    - enable: True
    - name: "cdp-vmagent"
    - watch:
      - file: /etc/systemd/system/cdp-vmagent.service
      - file: /opt/cdp-vmagent/prometheus.yml
{%- if monitoring.password %}
      - file: /opt/cdp-vmagent/remote_pwd_file
{%- elif monitoring.token %}
      - file: /opt/cdp-vmagent/remote_token_file
{%- endif %}
{%- endif %}