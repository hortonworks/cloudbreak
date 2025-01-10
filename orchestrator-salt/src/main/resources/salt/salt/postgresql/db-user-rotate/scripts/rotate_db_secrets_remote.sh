#!/usr/bin/env bash

rotationphase=$1

{%- from 'postgresql/settings.sls' import postgresql with context %}
{%- set rotation = salt['pillar.get']('postgres-rotation') %}

{% for service, values in pillar.get('postgres', {}).items() %}

{% for rotationservice, rotationvalues in rotation.items() %}

{% if service == rotationservice %}

oldusername="{{ rotationvalues['oldUser'] }}"
newusername="{{ rotationvalues['newUser'] }}"
admin_username="{{ values['remote_admin'] }}"
remotedburl="{{ values['remote_db_url'] }}"
remotedbport="{{ values['remote_db_port'] }}"
dbname="{{ values['database'] }}"
service="{{ service }}"
remotedbpass="{{ values['remote_admin_pw'] }}"
oldpassword="{{ rotationvalues['oldPassword'] }}"
newpassword="{{ rotationvalues['newPassword'] }}"

set -e

if [[ "$rotationphase" == "rotation" ]];then
    echo "$(date '+%d/%m/%Y %H:%M:%S') - Create user $newusername."
    if [ -z "$(PGPASSWORD={{ values['remote_admin_pw'] }} psql --host={{ values['remote_db_url'] }} --port={{ values['remote_db_port'] }} --username={{ values['remote_admin'] }} -d {{ values['database'] }} -tXAc "SELECT rolname FROM pg_roles" | grep {{ rotationvalues['newUser'] }} )" ]; then
        PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXAc \
            "CREATE USER $newusername WITH PASSWORD '$newpassword';"
    fi
    PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXA \
        -c "GRANT ALL PRIVILEGES ON DATABASE $dbname TO $newusername;" \
        -c "select pg_backend_pid();"
    PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXA \
        -c "GRANT $newusername TO $admin_username;" \
        -c "select pg_backend_pid();"
    # Check if the user is the owner of the schema
    IS_OWNER=$(PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXAc "SELECT CASE WHEN nspowner = (SELECT oid FROM pg_roles WHERE rolname = '$oldusername') THEN 'yes' ELSE 'no' END FROM pg_namespace WHERE nspname = 'public';")
    if [ "$IS_OWNER" == "yes" ]; then
        PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXA \
              -c "ALTER SCHEMA public OWNER TO $newusername;"  \
              -c "select pg_backend_pid();"
    else
        echo "GRANT USAGE, CREATE ON SCHEMA public TO $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON FUNCTIONS TO $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    fi
    PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXA \
        -c "REASSIGN OWNED BY $oldusername TO $newusername;" \
        -c "select pg_backend_pid();"
    echo "$(date '+%d/%m/%Y %H:%M:%S') - For debugging reasons, querying blocked processes."
    PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -c \
      "select pid, usename, pg_blocking_pids(pid) as blocked_by from pg_stat_activity where cardinality(pg_blocking_pids(pid)) > 0;"
fi

if [[ "$rotationphase" == "rollback" ]];then
    echo "$(date '+%d/%m/%Y %H:%M:%S') - Rolling back old user $oldusername."
    PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXAc \
            "ALTER SCHEMA public OWNER TO $oldusername;"
    if [ ! -z "$(PGPASSWORD={{ values['remote_admin_pw'] }} psql --host={{ values['remote_db_url'] }} --port={{ values['remote_db_port'] }} --username={{ values['remote_admin'] }} -d {{ values['database'] }} -tXAc "SELECT rolname FROM pg_roles" | grep {{ rotationvalues['newUser'] }} )" ]; then
        echo "REVOKE USAGE, CREATE ON SCHEMA public FROM $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL PRIVILEGES ON TABLES FROM $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL PRIVILEGES ON SEQUENCES FROM $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL PRIVILEGES ON FUNCTIONS FROM $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXA \
          -c "REASSIGN OWNED BY $newusername TO $oldusername;" \
          -c "select pg_backend_pid();"
        PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXA \
          -c "REVOKE $newusername FROM $admin_username;" \
          -c "select pg_backend_pid();"
        PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXA \
          -c "REVOKE ALL PRIVILEGES ON DATABASE $dbname FROM $newusername;" \
          -c "select pg_backend_pid();"
        PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXA \
          -c "DROP USER IF EXISTS $newusername;" \
          -c "select pg_backend_pid();"
        echo "$(date '+%d/%m/%Y %H:%M:%S') - For debugging reasons, querying blocked processes."
        PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -c \
          "select pid, usename, pg_blocking_pids(pid) as blocked_by from pg_stat_activity where cardinality(pg_blocking_pids(pid)) > 0;"
    fi
fi

if [[ "$rotationphase" == "finalize" ]];then
    echo "$(date '+%d/%m/%Y %H:%M:%S') - Removing old user $oldusername."
    if [ ! -z "$(PGPASSWORD={{ values['remote_admin_pw'] }} psql --host={{ values['remote_db_url'] }} --port={{ values['remote_db_port'] }} --username={{ values['remote_admin'] }} -d {{ values['database'] }} -tXAc "SELECT rolname FROM pg_roles" | grep {{ rotationvalues['oldUser'] }} )" ]; then
        echo "REVOKE USAGE, CREATE ON SCHEMA public FROM $oldusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM $oldusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM $oldusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM $oldusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL PRIVILEGES ON TABLES FROM $oldusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL PRIVILEGES ON SEQUENCES FROM $oldusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL PRIVILEGES ON FUNCTIONS FROM $oldusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXA \
          -c "REVOKE $oldusername FROM $admin_username;" \
          -c "select pg_backend_pid();"
        PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXA \
          -c "REVOKE ALL PRIVILEGES ON DATABASE $dbname FROM $oldusername;" \
          -c "select pg_backend_pid();"
        PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -tXA \
          -c "DROP USER IF EXISTS $oldusername;" \
          -c "select pg_backend_pid();"
        echo "$(date '+%d/%m/%Y %H:%M:%S') - For debugging reasons, querying blocked processes."
        PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname -c \
          "select pid, usename, pg_blocking_pids(pid) as blocked_by from pg_stat_activity where cardinality(pg_blocking_pids(pid)) > 0;"
    fi
fi

if [[ "$rotationphase" == "prevalidate" ]];then
    echo "$(date '+%d/%m/%Y %H:%M:%S') - Check for possible locks/blocking processes in database $dbname."
    LOCK_COUNT=$(PGPASSWORD={{ values['remote_admin_pw'] }} psql --host={{ values['remote_db_url'] }} --port={{ values['remote_db_port'] }} --username={{ values['remote_admin'] }} -d {{ values['database'] }} -tXAc "select count(*) from pg_stat_activity where cardinality(pg_blocking_pids(pid)) > 0;")
    if [[ ${LOCK_COUNT} > 0 ]]; then
        echo "$(date '+%d/%m/%Y %H:%M:%S') - There is at least one active lock for database $dbname, thus rotation would fail."
        return 1
    fi
fi

if [[ "$rotationphase" == "postvalidate" ]];then
    echo "$(date '+%d/%m/%Y %H:%M:%S') - No post validation for secrets rotation of database $dbname."
fi

set +e

{% endif %}

{% endfor %}

{% endfor %}
