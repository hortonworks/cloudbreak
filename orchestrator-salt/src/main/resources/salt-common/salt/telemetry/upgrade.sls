{%- from 'telemetry/settings.sls' import telemetry with context %}
{% if salt['pillar.get']('cloudera-manager:paywall_username') %}
  {% set curl_cmd = 'curl --max-time 30 -s -k -f -u $(grep username= /etc/yum.repos.d/cdp-infra-tools.repo | cut -d = -f2):$(grep password= /etc/yum.repos.d/cdp-infra-tools.repo | cut -d = -f2) ' + telemetry.repoGpgKey %}
{% else %}
  {% set curl_cmd = 'curl --max-time 30 -s -k -f ' + telemetry.repoGpgKey %}
{% endif %}
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
        - onlyif: "{{ curl_cmd }} > /dev/null"
    {%- if telemetry.proxyUrl %}
        - env:
          - https_proxy: {{ telemetry.proxyUrl }}
          {%- if telemetry.noProxyHosts %}
          - no_proxy: {{ telemetry.noProxyHosts }}
          {%- endif %}
    {%- endif %}
{%- endif %}