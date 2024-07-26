{%- from 'monitoring/settings.sls' import monitoring with context %}
{%- set is_salt_master = salt['file.directory_exists' ]('/srv/salt') %}
{%- set logging_types_arr = [] %}
{%- if salt['pillar.get']('fluent:cloudStorageLoggingEnabled') %}
  {%- set platform = salt['pillar.get']('fluent:platform') %}
  {%- if platform == "AWS" %}
    {% do logging_types_arr.append("s3") %}
  {%- elif platform == "AZURE" %}
    {% do logging_types_arr.append("abfs") %}
  {%- elif platform == "GCP" %}
    {% do logging_types_arr.append("gcs") %}
  {%- endif %}
{%- endif %}

/var/lib/node_exporter/scripts/salt.sh:
  file.managed:
    - source: salt://monitoring/scripts/salt.sh
    - user: "root"
    - group: "root"
    - mode: 700


{%- if logging_types_arr %}
/var/lib/node_exporter/scripts/logging-agent.sh:
  file.managed:
    - source: salt://monitoring/scripts/logging-agent.sh.j2
    - user: "root"
    - group: "root"
    - template: jinja
    - mode: 700
    - context:
        loggingTypes: {{ logging_types_arr|join(' ')}}

logging_agent_cron:
  cron.present:
    - name: /var/lib/node_exporter/scripts/logging-agent.sh
    - user: root
    - minute: '*/20'
    - require:
        - file: /var/lib/node_exporter/scripts/logging-agent.sh
{%- endif %}

salt_cron:
  cron.present:
    - name: /var/lib/node_exporter/scripts/salt.sh
    - user: root
    - minute: '*/5'
    - require:
        - file: /var/lib/node_exporter/scripts/salt.sh

{%- if is_salt_master %}
/var/lib/node_exporter/scripts/salt-key.sh:
  file.managed:
    - source: salt://monitoring/scripts/salt-key.sh
    - user: "root"
    - group: "root"
    - mode: 700

salt_key_cron:
  cron.present:
    - name: /var/lib/node_exporter/scripts/salt-key.sh
    - user: root
    - minute: '*/20'
    - require:
        - file: /var/lib/node_exporter/scripts/salt-key.sh

{%- endif %}