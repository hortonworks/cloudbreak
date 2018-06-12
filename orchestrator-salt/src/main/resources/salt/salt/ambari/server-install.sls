{%- from 'ambari/settings.sls' import ambari with context %}

include:
  - ambari.repo

ambari-server:
  pkg.installed:
    - require:
      - sls: ambari.repo
{% if grains['os_family'] == 'Debian' %}
    - skip_verify: True
{% endif %}
