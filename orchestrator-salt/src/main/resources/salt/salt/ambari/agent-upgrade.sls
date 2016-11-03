{%- from 'ambari/settings.sls' import ambari with context %}

include:
  - ambari.repo

stop-ambari-agent:
  service.dead:
    - name: ambari-agent

upgrade-ambari-agent:
  pkg.installed:
    - name: ambari-agent
    - require:
      - sls: ambari.repo
    - version: {{ ambari.version }}

upgrade-ambari-metrics-monitor:
  pkg.installed:
    - name: ambari-metrics-monitor
    - require:
      - sls: ambari.repo
    - version: {{ ambari.version }}
    - onlyif: rpm -qa ambari-metrics-monitor | grep ambari

upgrade-ambari-metrics-hadoop-sink:
  pkg.installed:
    - name: ambari-metrics-hadoop-sink
    - require:
      - sls: ambari.repo
    - version: {{ ambari.version }}
    - onlyif: rpm -qa ambari-metrics-hadoop-sink | grep ambari

upgrade-ambari-metrics-collector:
  pkg.installed:
    - name: ambari-metrics-collector
    - require:
      - sls: ambari.repo
    - version: {{ ambari.version }}
    - onlyif: rpm -qa ambari-metrics-collector | grep ambari