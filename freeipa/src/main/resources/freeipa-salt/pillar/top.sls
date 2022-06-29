base:
  '*':
    - telemetry.init
    - tags
    - databus
    - fluent
    - nodestatus
    - monitoring
    - discovery
    - proxy.proxy
    - freeipa
    - freeipa.services
    - upgradeccm
{%- if salt['file.file_exists']('/srv/pillar/recipes/init.sls') %}
    - recipes
{%- endif %}
