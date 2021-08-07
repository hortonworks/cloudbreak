#!/usr/bin/env bash

{%- from 'postgresql/settings.sls' import postgresql with context %}
{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE=verify-full
{%- endif %}

{% for service, values in pillar.get('postgres', {}).items() %}

{% if values['user'] is defined %}


echo "Check if database already exists for {{ service }}"
PGPASSWORD={{ values['remote_admin_pw'] }} psql --host={{ values['remote_db_url'] }} --port={{ values['remote_db_port'] }} --username={{ values['remote_admin'] }} -v "ON_ERROR_STOP=1" -lqt | awk {'print $1'} | grep -qw {{ values['database'] }}
result=$?

set -e
if [[ $result -eq 0 ]]; then
    echo "Database already exists for {{ service }}, skipping initialization."
else
    username="{{ values['user'] }}"
    username="${username%%@*}"
    admin_username="{{ values['remote_admin'] }}"
    admin_username="${admin_username%%@*}"
    echo "Create remote database and user for service {{ service }}"
    PGPASSWORD={{ values['remote_admin_pw'] }} createdb --host={{ values['remote_db_url'] }} --port={{ values['remote_db_port'] }} --username={{ values['remote_admin'] }} {{ values['database'] }}
    echo "CREATE USER $username WITH PASSWORD '{{ values['password'] }}';" | PGPASSWORD={{ values['remote_admin_pw'] }} psql --host={{ values['remote_db_url'] }} --port={{ values['remote_db_port'] }} --username={{ values['remote_admin'] }} -v "ON_ERROR_STOP=1" {{ values['database'] }}
    echo "GRANT ALL PRIVILEGES ON DATABASE {{ values['database'] }} TO $username;" | PGPASSWORD={{ values['remote_admin_pw'] }} psql --host={{ values['remote_db_url'] }} --port={{ values['remote_db_port'] }} --username={{ values['remote_admin'] }} -v "ON_ERROR_STOP=1" {{ values['database'] }}
    echo "GRANT $username TO $admin_username" | PGPASSWORD={{ values['remote_admin_pw'] }} psql --host={{ values['remote_db_url'] }} --port={{ values['remote_db_port'] }} --username={{ values['remote_admin'] }} -v "ON_ERROR_STOP=1" {{ values['database'] }}
    echo "ALTER SCHEMA public OWNER TO $username" | PGPASSWORD={{ values['remote_admin_pw'] }} psql --host={{ values['remote_db_url'] }} --port={{ values['remote_db_port'] }} --username={{ values['remote_admin'] }} -v "ON_ERROR_STOP=1" {{ values['database'] }}
fi
set +e

{% endif %}

{% endfor %}
