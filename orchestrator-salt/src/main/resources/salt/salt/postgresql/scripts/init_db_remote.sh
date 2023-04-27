#!/usr/bin/env bash

create_user() {
  username="$1"
  admin_username="$2"
  remotedburl="$3"
  remotedbport="$4"
  dbname="$5"
  service="$6"
  remotedbpass="$7"
  password=$8

  echo "CREATE USER $username WITH PASSWORD '$password';" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
  echo "GRANT ALL PRIVILEGES ON DATABASE $dbname TO $username;" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
  echo "GRANT $username TO $admin_username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
  echo "ALTER SCHEMA public OWNER TO $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
}

{%- from 'postgresql/settings.sls' import postgresql with context %}
{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE=verify-full
{%- endif %}

{% for service, values in pillar.get('postgres', {}).items() %}

{% if values['user'] is defined %}

username="{{ values['user'] }}"
username="${username%%@*}"
admin_username="{{ values['remote_admin'] }}"
admin_username="${admin_username%%@*}"
remotedburl="{{ values['remote_db_url'] }}"
remotedbport="{{ values['remote_db_port'] }}"
dbname="{{ values['database'] }}"
service="{{ service }}"
remotedbpass="{{ values['remote_admin_pw'] }}"
password="{{ values['password'] }}"

echo "Check if database already exists for $service"
PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" -lqt | awk {'print $1'} | grep -qw $dbname
result=$?

if [[ $result -eq 0 ]]; then
    echo "Database already exists for $service, skipping initialization, checking if user exists."
    PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username --dbname=$dbname -v "ON_ERROR_STOP=1" -qt -c 'SELECT rolname FROM pg_roles;' | grep -qw $username
    result=$?
    if [[ $result -eq 0 ]]; then
        echo "User $username already exists, skipping user creation."
    else
        set -e
        echo "Create user $username."
        create_user $username $admin_username $remotedburl $remotedbport $dbname $service $remotedbpass $password
        oldusername=$(PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username --dbname=$dbname -v "ON_ERROR_STOP=1" -qt -c "SELECT tableowner FROM pg_tables WHERE tablename = 'schema_version'")
        echo "REASSIGN OWNED BY $oldusername TO $username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        set +e
    fi
else
    set -e
    echo "Create remote database and user for service $service"
    PGPASSWORD=$remotedbpass createdb --host=$remotedburl --port=$remotedbport --username=$admin_username $dbname
    create_user $username $admin_username $remotedburl $remotedbport $dbname $service $remotedbpass $password
    set +e
fi

{% endif %}

{% endfor %}
