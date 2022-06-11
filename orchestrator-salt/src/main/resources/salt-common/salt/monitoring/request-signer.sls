{%- from 'monitoring/settings.sls' import monitoring with context %}
{%- if monitoring.enabled and monitoring.requestSignerEnabled and monitoring.databusEnabled %}
{% set request_signer_installed = salt['file.directory_exists' ]('/opt/cdp-request-signer/bin') %}
{%- if not request_signer_installed %}
install_request_signer:
  cmd.run:
    - name: "yum install -y cdp-request-signer --disablerepo='*' --enablerepo=cdp-infra-tools"
    - onlyif: "! rpm -q cdp-request-signer"
{%- endif %}

generate_request_signer_cert_and_key:
  cmd.run:
    - name: "/opt/salt/scripts/cert-helper.sh -b /opt/cdp-request-signer/conf/request-signer"

/opt/cdp-request-signer/conf/databus_credential:
  file.managed:
    - source: salt://monitoring/template/databus_credential.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600

/opt/cdp-request-signer/conf/cdp-request-signer.yaml:
  file.managed:
    - source: salt://monitoring/template/cdp-request-signer.yaml.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 600

/etc/systemd/system/cdp-request-signer.service:
  file.managed:
    - source: salt://monitoring/systemd/cdp-request-signer.service.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: 640

start_cdp_request_signer:
  service.running:
    - enable: True
    - name: "cdp-request-signer"
    - watch:
      - file: /etc/systemd/system/cdp-request-signer.service
      - file: /opt/cdp-request-signer/conf/cdp-request-signer.yaml
{%- endif %}
