#!/usr/bin/env bash

{%- from 'postgresql/settings.sls' import postgresql with context %}
{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE=verify-full
{%- endif %}

{% set recovery_reused_dbs =  salt['pillar.get']('postgres:recovery_reused_databases') %}

{% for service, values in pillar.get('postgres', {}).items() %}
{% if values['user'] is defined %}

{% if service in recovery_reused_dbs %}
    echo "Keeping database for {{ service }}"

{% else %}
    username="{{ values['user'] }}"
    username="${username%%@*}"
    set -e
    echo "Deleting remote database and user for service {{ service }}"
    PGPASSWORD={{ values['remote_admin_pw'] }} dropdb --host={{ values['remote_db_url'] }} --port={{ values['remote_db_port'] }} --username={{ values['remote_admin'] }} --if-exists {{ values['database'] }}
    echo "DROP USER IF EXISTS $username;" | PGPASSWORD={{ values['remote_admin_pw'] }} psql --host={{ values['remote_db_url'] }} --port={{ values['remote_db_port'] }} --username={{ values['remote_admin'] }} -v "ON_ERROR_STOP=1" postgres
    set +e

{% endif %}
{% endif %}

{% endfor %}
