{%- from 'telemetry/settings.sls' import telemetry with context %}
{%- from 'monitoring/settings.sls' import monitoring with context %}

{%- if monitoring.enabled %}
{% set vmagent_installed = salt['file.directory_exists' ]('/opt/cdp-vmagent/bin') %}
{%- if not vmagent_installed %}
install_vmagent:
  cmd.run:
    - name: "yum install -y cdp-vmagent --disablerepo='*' --enablerepo=cdp-infra-tools"
    - onlyif: "! rpm -q cdp-vmagent"
{%- endif %}

generate_vmagent_cert_and_key:
  cmd.run:
    - name: "/opt/salt/scripts/cert-helper.sh -b /opt/cdp-vmagent/conf/vmagent"

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
    - mode: 600

{%- if monitoring.requestSignerEnabled %}
/opt/cdp-vmagent/request_signer_pwd_file:
  file.managed:
    - source: salt://monitoring/template/request_signer_pwd_file.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600
{%- else %}
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
{%- endif %}

start_cdp_vmagent:
  service.running:
    - enable: True
    - name: "cdp-vmagent"
    - watch:
      - file: /etc/systemd/system/cdp-vmagent.service
      - file: /opt/cdp-vmagent/prometheus.yml
{%- if monitoring.requestSignerEnabled %}
      - file: /opt/cdp-vmagent/request_signer_pwd_file
{%- else %}
{%- if monitoring.password %}
      - file: /opt/cdp-vmagent/remote_pwd_file
{%- elif monitoring.token %}
      - file: /opt/cdp-vmagent/remote_token_file
{%- endif %}
{%- endif %}
{%- endif %}