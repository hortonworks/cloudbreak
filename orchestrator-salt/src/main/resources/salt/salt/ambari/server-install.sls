{%- from 'ambari/settings.sls' import ambari with context %}

include:
  - ambari.repo

ambari-server:
  pkg.installed:
    - require:
      - sls: ambari.repo