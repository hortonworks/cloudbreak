{%- from 'ambari/settings.sls' import ambari with context %}

include:
  - ambari.repo

upgrade-smartsense:
  pkg.latest:
    - name: smartsense-hst
    - require:
      - sls: ambari.repo
    - onlyif: rpm -qa smartsense-hst | grep smart