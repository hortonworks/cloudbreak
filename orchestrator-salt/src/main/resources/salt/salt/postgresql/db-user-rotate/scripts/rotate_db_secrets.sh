#!/usr/bin/env bash
set -e

rotationphase=$1

CONFIG_DIR=$(psql -U postgres -c "show config_file;" -t | sed 's/\/postgresql.conf//g' | xargs)
echo "CONFIG_DIR: $CONFIG_DIR"

{%- from 'postgresql/settings.sls' import postgresql with context %}
{%- set rotation = salt['pillar.get']('postgres-rotation') %}

{% for service, values in pillar.get('postgres', {}).items()  %}

{% for rotationservice, rotationvalues in rotation.items() %}

{% if service == rotationservice %}

oldusername="{{ rotationvalues['oldUser'] }}"
newusername="{{ rotationvalues['newUser'] }}"
dbname="{{ values['database'] }}"
service="{{ service }}"
oldpassword="{{ rotationvalues['oldPassword'] }}"
newpassword="{{ rotationvalues['newPassword'] }}"

if [[ "$rotationphase" == "rotation" ]];then
    echo "$(date '+%d/%m/%Y %H:%M:%S') - Update user in $service database"
    if [ -z "$(psql -U postgres -d {{ values['database'] }} -tXAc "SELECT rolname FROM pg_roles" | grep {{ rotationvalues['newUser'] }} )" ]; then
        psql -U postgres -d $dbname -v "ON_ERROR_STOP=1" -tXA \
          -c "CREATE USER $newusername WITH PASSWORD '$newpassword';" \
          -c "select pg_backend_pid();"
    fi
    psql -U postgres -d $dbname -v "ON_ERROR_STOP=1" -tXA \
      -c  "GRANT ALL PRIVILEGES ON DATABASE $dbname TO $newusername;" \
      -c "select pg_backend_pid();"
    psql -U postgres -d $dbname -v "ON_ERROR_STOP=1" -tXA \
      -c  "REASSIGN OWNED BY $oldusername TO $newusername;" \
      -c "select pg_backend_pid();"
    echo "$(date '+%d/%m/%Y %H:%M:%S') - For debugging reasons, querying blocked processes."
    psql -U postgres -d $dbname -v "ON_ERROR_STOP=1" -c \
      "select pid, usename, pg_blocking_pids(pid) as blocked_by from pg_stat_activity where cardinality(pg_blocking_pids(pid)) > 0;"

    echo "$(date '+%d/%m/%Y %H:%M:%S') - Add access to pg_hba.conf"
    {% if postgresql.ssl_enabled == True %}
    sed -i '1ihostssl {{ values['database'] }} {{ rotationvalues['newUser'] }} 0.0.0.0/0 md5' $CONFIG_DIR/pg_hba.conf
    {%- else %}
    sed -i '1ihost {{ values['database'] }} {{ rotationvalues['newUser'] }} 0.0.0.0/0 md5' $CONFIG_DIR/pg_hba.conf
    {%- endif %}
    sed -i '1ilocal {{ values['database'] }} {{ rotationvalues['newUser'] }} md5' $CONFIG_DIR/pg_hba.conf
fi

if [[ "$rotationphase" == "rollback" ]];then
    echo "$(date '+%d/%m/%Y %H:%M:%S') - Rolling back old user $oldusername."
    if [ ! -z "$(psql -U postgres -d {{ values['database'] }} -tXAc "SELECT rolname FROM pg_roles" | grep {{ rotationvalues['newUser'] }} )" ]; then
        psql -U postgres -d $dbname -v "ON_ERROR_STOP=1" -tXA \
          -c "REASSIGN OWNED BY $newusername TO $oldusername;" \
          -c "select pg_backend_pid();"
        psql -U postgres -d $dbname -v "ON_ERROR_STOP=1" -tXA \
          -c "REVOKE ALL PRIVILEGES ON DATABASE $dbname FROM $newusername;" \
          -c "select pg_backend_pid();"
        echo "DROP USER IF EXISTS $newusername;"
        echo "$(date '+%d/%m/%Y %H:%M:%S') - For debugging reasons, querying blocked processes."
        psql -U postgres -d $dbname -v "ON_ERROR_STOP=1" -c \
          "select pid, usename, pg_blocking_pids(pid) as blocked_by from pg_stat_activity where cardinality(pg_blocking_pids(pid)) > 0;"
    fi
fi

if [[ "$rotationphase" == "finalize" ]];then
    echo "$(date '+%d/%m/%Y %H:%M:%S') - Removing old user $oldusername."
    if [ ! -z "$(psql -U postgres -d {{ values['database'] }} -tXAc "SELECT rolname FROM pg_roles" | grep {{ rotationvalues['oldUser'] }} )" ]; then
        psql -U postgres -d $dbname -v "ON_ERROR_STOP=1" -tXA \
          -c "REVOKE ALL PRIVILEGES ON DATABASE $dbname FROM $oldusername;" \
          -c "select pg_backend_pid();"
        psql -U postgres -d $dbname -v "ON_ERROR_STOP=1" -tXA \
          -c "DROP USER IF EXISTS $oldusername;" \
          -c "select pg_backend_pid();"
        echo "$(date '+%d/%m/%Y %H:%M:%S') - For debugging reasons, querying blocked processes."
        psql -U postgres -d $dbname -v "ON_ERROR_STOP=1" -c \
          "select pid, usename, pg_blocking_pids(pid) as blocked_by from pg_stat_activity where cardinality(pg_blocking_pids(pid)) > 0;"
    fi
fi

if [[ "$rotationphase" == "prevalidate" ]];then
    echo "$(date '+%d/%m/%Y %H:%M:%S') - Check for possible locks/blocking processes in database $dbname."
    LOCK_COUNT=$(psql -U postgres -d {{ values['database'] }} -tXAc "select count(*) from pg_stat_activity where cardinality(pg_blocking_pids(pid)) > 0;")
    if [[ ${LOCK_COUNT} > 0 ]]; then
        echo "$(date '+%d/%m/%Y %H:%M:%S') - There is at least one active lock for database $dbname, thus rotation would fail."
        return 1
    fi
fi

if [[ "$rotationphase" == "postvalidate" ]];then
    echo "$(date '+%d/%m/%Y %H:%M:%S') - No post validation for secrets rotation of database $dbname."
fi

{% endif %}

{% endfor %}

{% endfor %}

set +e
