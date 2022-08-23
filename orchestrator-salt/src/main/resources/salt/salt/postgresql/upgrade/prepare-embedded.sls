{% set original_postgres_version = salt['pillar.get']('postgres:upgrade:original_postgres_version', '10') %}
{% set new_postgres_version = salt['pillar.get']('postgres:upgrade:new_postgres_version', '11') %}
{% set original_postgres_binaries = salt['pillar.get']('postgres:upgrade:original_postgres_binaries', '/usr/pgsql-10') %}
{% set temp_directory = salt['pillar.get']('postgres:upgrade:temp_directory', '/dbfs/tmp') %}

{% set command = "{ psql -U postgres -c 'show server_version;' -t 2>/dev/null || echo Unknown; } | { grep -o '[0-9]*\.' || echo Unknown; }" %}
{% set running_postgres_version = salt['cmd.shell'](command) | replace(".", "") %}

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
