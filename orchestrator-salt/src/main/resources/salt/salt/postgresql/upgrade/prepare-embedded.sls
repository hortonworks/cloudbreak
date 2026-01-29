{%- set original_postgres_version = salt['pillar.get']('postgres:upgrade:original_postgres_version', '10') %}
{%- set new_postgres_version = salt['pillar.get']('postgres:upgrade:new_postgres_version', '11') %}
{%- set original_postgres_binaries = salt['pillar.get']('postgres:upgrade:original_postgres_binaries', '/usr/pgsql-10') %}
{%- set temp_directory = salt['pillar.get']('postgres:upgrade:temp_directory', '/dbfs/tmp') %}

{%- set command = "{ psql -U postgres -c 'show server_version;' -t 2>/dev/null || echo Unknown; } | { grep -o '[0-9]*\.' || echo Unknown; }" %}
{%- set running_postgres_version = salt['cmd.shell'](command) | replace(".", "") %}

{%- do salt.log.debug("log_original_postgres_version " ~ original_postgres_version) %}
{%- do salt.log.debug("log_new_postgres_version " ~ new_postgres_version) %}
{%- do salt.log.debug("log_original_postgres_binaries " ~ original_postgres_binaries) %}
{%- do salt.log.debug("log_temp_directory " ~ temp_directory) %}
{%- do salt.log.debug("log_running_postgres_version " ~ running_postgres_version) %}

  {%- if new_postgres_version | int in [11, 14, 17] %}
install_target_postgres_version:
  module.run:
    - name: state.sls
    - mods: postgresql.pg-install
    - kwargs:
        pillar:
          postgres:
            postgres_version: {{ new_postgres_version }}
    # 1. Don't run if the destination binary already exists (File check)
    - unless: test -f /usr/pgsql-{{ new_postgres_version }}/bin/psql
{%- endif %}

{% if running_postgres_version == original_postgres_version %}

{{ temp_directory }}:
  file.directory:
    - user: root
    - group: root
    - mode: 755

{{ temp_directory }}/pgsql-{{ original_postgres_version }}:
  file.copy:
    - source: {{ original_postgres_binaries }}
    - user: postgres
    - group: postgres

{% elif running_postgres_version != new_postgres_version %}

failure:
  test.fail_without_changes:
    - name: "Postgres upgrade is not possible from {{ running_postgres_version }} to {{ new_postgres_version }}"
    - failhard: True

{% endif %}
