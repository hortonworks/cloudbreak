#!/usr/bin/env bash

{%- from 'postgresql/settings.sls' import postgresql with context %}
{%- set newuser = salt['pillar.get']('postgres-user') %}

{% for newuserservice, newuservalues in newuser.items() %}

{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE="{{ postgresql.ssl_verification_mode }}"
{%- endif %}

set -e
username="{{ newuservalues['user'] }}"
username="${username%%@*}"
admin_username="{{ newuservalues['remote_admin'] }}"
admin_username="${admin_username%%@*}"
password="{{ newuservalues['password'] }}"
remotedburl="{{ newuservalues['remote_db_url'] }}"
remotedbport="{{ newuservalues['remote_db_port'] }}"
dbname="{{ newuservalues['database'] }}"
remotedbpass="{{ newuservalues['remote_admin_pw'] }}"

echo "$(date '+%d/%m/%Y %H:%M:%S') - Create user $username in $dbname database."
if [ -z "$(PGPASSWORD={{ newuservalues['remote_admin_pw'] }} psql --host={{ newuservalues['remote_db_url'] }} --port={{ newuservalues['remote_db_port'] }} --username={{ newuservalues['remote_admin'] }} -d {{ newuservalues['database'] }} -tXAc "SELECT rolname FROM pg_roles" | grep {{ newuservalues['user'] }} )" ]; then
  echo "CREATE USER $username WITH PASSWORD '$password';" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
else
  echo "$(date '+%d/%m/%Y %H:%M:%S') - User $username is already in $dbname database."
fi
echo "GRANT ALL PRIVILEGES ON DATABASE $dbname TO $username;" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
echo "GRANT $username TO $admin_username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
echo "GRANT USAGE, CREATE ON SCHEMA public TO $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
echo "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
echo "GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
echo "GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON FUNCTIONS TO $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
echo "$(date '+%d/%m/%Y %H:%M:%S') - Creation of user $username in $dbname database finished."

set +e

{% endfor %}
