{%- from 'postgresql/settings.sls' import postgresql with context %}

{% set postgres_directory = salt['pillar.get']('postgres:postgres_directory') %}
{% set config_file = postgres_directory ~ '/data/postgresql.conf' %}
{% set postgres_log_directory = salt['pillar.get']('postgres:postgres_log_directory') %}
{% set postgres_scripts_executed_directory = salt['pillar.get']('postgres:postgres_scripts_executed_directory') %}

{% set marker_file = postgres_scripts_executed_directory ~ '/pgsql_ssl_configured' %}

{% for stale_file in [
    '/opt/salt/scripts/conf_pgsql_ssl.sh',
    postgres_log_directory ~ '/pgsql_ssl_configured'
] %}
remove-legacy-ssl-artifact-{{ loop.index }}:
  file.absent:
    - name: {{ stale_file }}
{% endfor %}

{%- set base_directives = [
    {'key': 'ssl', 'value': 'on', 'apply': True},
    {'key': 'ssl_cert_file', 'value': "'" ~ postgres_directory ~ "/certs/postgres.cert'", 'apply': True},
    {'key': 'ssl_key_file',  'value': "'" ~ postgres_directory ~ "/certs/postgres.key'", 'apply': True},
    {'key': 'ssl_ciphers',   'value': "'" ~ postgresql.tls12_ciphers ~ "'", 'apply': postgresql.tls12_ciphers | length > 0}
] %}

{%- if postgresql.tls_advanced_control %}
  {%- set advanced_directives = [
    {'key': 'ssl_prefer_server_ciphers', 'value': 'on', 'apply': True},
    {'key': 'ssl_min_protocol_version', 'value': "'" ~ postgresql.tls_min_version ~ "'", 'apply': postgresql.tls_min_version | length > 0},
    {'key': 'ssl_max_protocol_version', 'value': "'" ~ postgresql.tls_max_version ~ "'", 'apply': postgresql.tls_max_version | length > 0}
  ] %}
{%- else %}
  {%- set advanced_directives = [
    {'key': 'ssl_prefer_server_ciphers', 'value': '', 'apply': False},
    {'key': 'ssl_min_protocol_version', 'value': '', 'apply': False},
    {'key': 'ssl_max_protocol_version', 'value': '', 'apply': False}
  ] %}
{%- endif %}

{%- set all_directives = base_directives + advanced_directives %}

{#- The directives are applied only when the desired settings differ from the ones recorded in the marker file,
    so manual edits of postgresql.conf survive until the pillar values change (same as the former script). #}
{%- set fingerprint_parts = [] %}
{%- for d in all_directives %}
{%- do fingerprint_parts.append(d.key ~ '|' ~ d.apply ~ '|' ~ d.value) %}
{%- endfor %}
{%- set fingerprint = fingerprint_parts | join(';') | md5 %}
{%- set unchanged_check = "grep -qxF '" ~ fingerprint ~ "' " ~ marker_file %}

{%- for d in all_directives %}
{%- if d.apply %}
configure-ssl-{{ d.key }}:
  file.replace:
    - name: {{ config_file }}
    - pattern: '^{{ d.key }}[ \t]*=.*'
    - repl: "{{ d.key }} = {{ d.value }}"
    - append_if_not_found: True
    - backup: False
    - unless: {{ unchanged_check }}
    - require:
      - service: start-postgresql
{%- else %}
configure-ssl-{{ d.key }}:
  file.line:
    - name: {{ config_file }}
    - match: '^{{ d.key }}[ \t]*=.*'
    - mode: delete
    - backup: False
    - unless: {{ unchanged_check }}
    - require:
      - service: start-postgresql
{%- endif %}
{%- endfor %}

configure-ssl:
  cmd.run:
    - name: 'true'
    - onchanges:
{%- for d in all_directives %}
      - file: configure-ssl-{{ d.key }}
{%- endfor %}

record-ssl-settings-fingerprint:
  cmd.run:
    - name: echo '{{ fingerprint }}' > {{ marker_file }}
    - unless: {{ unchanged_check }}
    - require:
{%- for d in all_directives %}
      - file: configure-ssl-{{ d.key }}
{%- endfor %}
