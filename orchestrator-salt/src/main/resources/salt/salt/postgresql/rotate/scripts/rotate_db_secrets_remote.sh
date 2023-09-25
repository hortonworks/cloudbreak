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
        echo "CREATE USER $newusername WITH PASSWORD '$newpassword';" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    fi
    echo "GRANT ALL PRIVILEGES ON DATABASE $dbname TO $newusername;" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    echo "GRANT $newusername TO $admin_username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    echo "ALTER SCHEMA public OWNER TO $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    echo "REASSIGN OWNED BY $oldusername TO $newusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
fi

if [[ "$rotationphase" == "rollback" ]];then
    echo "$(date '+%d/%m/%Y %H:%M:%S') - Rolling back old user $oldusername."
    echo "ALTER SCHEMA public OWNER TO $oldusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    if [ ! -z "$(PGPASSWORD={{ values['remote_admin_pw'] }} psql --host={{ values['remote_db_url'] }} --port={{ values['remote_db_port'] }} --username={{ values['remote_admin'] }} -d {{ values['database'] }} -tXAc "SELECT rolname FROM pg_roles" | grep {{ rotationvalues['newUser'] }} )" ]; then
        echo "REASSIGN OWNED BY $newusername TO $oldusername" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "REVOKE $newusername FROM $admin_username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "REVOKE ALL PRIVILEGES ON DATABASE $dbname FROM $newusername;" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "DROP USER IF EXISTS $newusername;" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
    fi
fi

if [[ "$rotationphase" == "finalize" ]];then
    echo "$(date '+%d/%m/%Y %H:%M:%S') - Removing old user $oldusername."
    if [ ! -z "$(PGPASSWORD={{ values['remote_admin_pw'] }} psql --host={{ values['remote_db_url'] }} --port={{ values['remote_db_port'] }} --username={{ values['remote_admin'] }} -d {{ values['database'] }} -tXAc "SELECT rolname FROM pg_roles" | grep {{ rotationvalues['oldUser'] }} )" ]; then
        echo "REVOKE $oldusername FROM $admin_username" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "REVOKE ALL PRIVILEGES ON DATABASE $dbname FROM $oldusername;" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
        echo "DROP USER IF EXISTS $oldusername;" | PGPASSWORD=$remotedbpass psql --host=$remotedburl --port=$remotedbport --username=$admin_username -v "ON_ERROR_STOP=1" $dbname
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
