{%- from 'telemetry/settings.sls' import telemetry with context %}
{% set test_cmd = 'test -f /etc/yum.repos.d/cdp-infra-tools.repo && (' + telemetry.testInfraRepoCurlCmd + ')' %}
/opt/cdp-telemetry/conf/proxy.env:
  file.managed:
    - source: salt://telemetry/template/proxy.env.j2
    - template: jinja
    - makedirs: True
    - mode: 700

{%- if telemetry.repoName %}
/etc/yum.repos.d/cdp-infra-tools.repo:
  file.managed:
    - source: salt://telemetry/template/cdp-infra-tools.repo.j2
    - template: jinja
    - mode: 640
    - context:
         repoName: "{{ telemetry.repoName }}"
         repoBaseUrl: "{{ telemetry.repoBaseUrl }}"
         repoGpgKey: "{{ telemetry.repoGpgKey }}"
         repoGpgCheck: {{ telemetry.repoGpgCheck }}
{%- endif %}

/opt/salt/scripts/cdp-telemetry-deployer.sh:
    file.managed:
        - source: salt://telemetry/scripts/cdp-telemetry-deployer.sh
        - template: jinja
        - mode: '0750'
{%- if telemetry.desiredCdpTelemetryVersion or telemetry.desiredCdpLoggingAgentVersion or telemetry.desiredCdpRequestSignerVersion %}
upgrade_cdp_infra_tools_components:
    cmd.run:
        - names:
{%- if telemetry.desiredCdpTelemetryVersion %}
          - /bin/bash -c 'source /opt/cdp-telemetry/conf/proxy.env; /opt/salt/scripts/cdp-telemetry-deployer.sh upgrade -c cdp-telemetry -v {{ telemetry.desiredCdpTelemetryVersion }}';exit 0
{%- endif %}
{%- if telemetry.desiredCdpLoggingAgentVersion %}
          - /bin/bash -c 'source /opt/cdp-telemetry/conf/proxy.env; /opt/salt/scripts/cdp-telemetry-deployer.sh upgrade -c cdp-logging-agent -v {{ telemetry.desiredCdpLoggingAgentVersion }}';exit 0
{%- endif %}
{%- if telemetry.desiredCdpRequestSignerVersion %}
          - /bin/bash -c 'source /opt/cdp-telemetry/conf/proxy.env; /opt/salt/scripts/cdp-telemetry-deployer.sh upgrade -c cdp-request-signer -v {{ telemetry.desiredCdpRequestSignerVersion }}';exit 0
{%- endif %}
        - onlyif: source /opt/cdp-telemetry/conf/proxy.env; {{ test_cmd }}
{%- endif %}