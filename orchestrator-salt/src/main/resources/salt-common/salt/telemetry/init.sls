{%- from 'telemetry/settings.sls' import telemetry with context %}
{% if telemetry.databusEndpointValidation and telemetry.databusEndpoint %}
check_databus_network_connectivity:
  cmd.run:
    - name: "curl {{ telemetry.databusCurlConnectOpts }} -s -k {{ telemetry.databusEndpoint }} > /dev/null"
    - failhard: True
    {%- if telemetry.proxyUrl %}
    - env:
       - https_proxy: {{ telemetry.proxyUrl }}
       {%- if telemetry.noProxyHosts %}
       - no_proxy: {{ telemetry.noProxyHosts }}
       {%- endif %}
    {%- endif %}
{%- endif %}

{%- if telemetry.noProxyHosts and telemetry.cdpTelemetryVersion > 8 %}
/etc/cdp-telemetry/conf:
  file.directory:
    - makedirs: True

/etc/cdp-telemetry/conf/proxy-whitelist.txt:
    file.managed:
        - source: salt://telemetry/template/proxy-whitelist.txt.j2
        - template: jinja
        - mode: '0640'
{%- endif %}
include:
  - telemetry.upgrade

{%- if grains['os_family'] == 'RedHat' and grains['osmajorrelease'] | int >= 8 %}
remove_cdp_telemetry_libcrypto:
    file.absent:
        - name: /opt/cdp-telemetry/bin/libcrypto.so.1.1
        - onlyif: test -f /lib64/libcrypto.so.1.1
{%- endif %}
