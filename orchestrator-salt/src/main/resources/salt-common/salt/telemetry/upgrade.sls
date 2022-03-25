{%- from 'telemetry/settings.sls' import telemetry with context %}
{% set test_cmd = 'test -f /etc/yum.repos.d/cdp-infra-tools.repo && (' + telemetry.testInfraRepoCurlCmd + ')' %}
{% set test_delete_cmd = 'test -f /etc/yum.repos.d/cdp-infra-tools.repo && ! (' + telemetry.testInfraRepoCurlCmd + ')' %}
{%- if telemetry.repoName and (telemetry.desiredCdpTelemetryVersion or telemetry.desiredCdpLoggingAgentVersion) %}
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
delete_repo_file:
  file.absent:
    - name: /etc/yum.repos.d/cdp-infra-tools.repo
    - onlyif: {{ test_delete_cmd }}
    {%- if telemetry.proxyUrl %}
    - env:
       - https_proxy: {{ telemetry.proxyUrl }}
       {%- if telemetry.noProxyHosts %}
       - no_proxy: {{ telemetry.noProxyHosts }}
       {%- endif %}
    {%- endif %}

/opt/salt/scripts/cdp-telemetry-deployer.sh:
    file.managed:
        - source: salt://telemetry/scripts/cdp-telemetry-deployer.sh
        - template: jinja
        - mode: '0750'
{%- if telemetry.desiredCdpTelemetryVersion or telemetry.desiredCdpLoggingAgentVersion %}
upgrade_cdp_infra_tools_components:
    cmd.run:
        - names:
{%- if telemetry.desiredCdpTelemetryVersion %}
          - /bin/bash -c '/opt/salt/scripts/cdp-telemetry-deployer.sh upgrade -c cdp-telemetry -v {{ telemetry.desiredCdpTelemetryVersion }}';exit 0
{%- endif %}
{%- if telemetry.desiredCdpLoggingAgentVersion %}
          - /bin/bash -c '/opt/salt/scripts/cdp-telemetry-deployer.sh upgrade -c cdp-logging-agent -v {{ telemetry.desiredCdpLoggingAgentVersion }}';exit 0
{%- endif %}
        - onlyif: {{ test_cmd }}
    {%- if telemetry.proxyUrl %}
        - env:
          - https_proxy: {{ telemetry.proxyUrl }}
          {%- if telemetry.noProxyHosts %}
          - no_proxy: {{ telemetry.noProxyHosts }}
          {%- endif %}
    {%- endif %}
{%- endif %}