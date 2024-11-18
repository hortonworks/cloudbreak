#!/usr/bin/env bash

{%- from 'postgresql/settings.sls' import postgresql with context %}
{%- set deleteuser = salt['pillar.get']('postgres-user') %}

{% for deleteuserservice, deleteuservalues in deleteuser.items() %}

{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE="{{ postgresql.ssl_verification_mode }}"
{%- endif %}

set -e
username="{{ deleteuservalues['user'] }}"
username="${username%%@*}"
admin_username="{{ deleteuservalues['remote_admin'] }}"
admin_username="${admin_username%%@*}"
remotedburl="{{ deleteuservalues['remote_db_url'] }}"
remotedbport="{{ deleteuservalues['remote_db_port'] }}"
dbname="{{ deleteuservalues['database'] }}"
remotedbpass="{{ deleteuservalues['remote_admin_pw'] }}"

if [ ! -z "$(PGPASSWORD={{ deleteuservalues['remote_admin_pw'] }} psql --host={{ deleteuservalues['remote_db_url'] }} --port={{ deleteuservalues['remote_db_port'] }} --username={{ deleteuservalues['remote_admin'] }} -d {{ deleteuservalues['database'] }} -tXAc "SELECT rolname FROM pg_roles" | grep $username )" ]; then
  IS_OWNER=$(PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXAc "SELECT CASE WHEN nspowner = (SELECT oid FROM pg_roles WHERE rolname = '$oldusername') THEN 'yes' ELSE 'no' END FROM pg_namespace WHERE nspname = 'public';")
  if [ "$IS_OWNER" == "yes" ]; then
    echo "$(date '+%d/%m/%Y %H:%M:%S') We do not support direct removal of schema owner $username in $dbname database, thus skipping removal! Rotation of the schema owner is possible."
  else
    echo "$(date '+%d/%m/%Y %H:%M:%S') - Delete user $username from $dbname database."
    echo "REVOKE USAGE, CREATE ON SCHEMA public FROM $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    echo "REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    echo "REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    echo "REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL PRIVILEGES ON TABLES FROM $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL PRIVILEGES ON SEQUENCES FROM $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL PRIVILEGES ON FUNCTIONS FROM $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    echo "REVOKE ALL PRIVILEGES ON DATABASE $dbname FROM $username;" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    echo "REVOKE $username FROM $admin_username;" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    echo "DROP USER IF EXISTS $username;" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    echo "$(date '+%d/%m/%Y %H:%M:%S') - Deletion of user $username from $dbname database finished."
  fi
else
  echo "$(date '+%d/%m/%Y %H:%M:%S') - User $username already deleted from $dbname database."
fi

set +e

{% endfor %}
