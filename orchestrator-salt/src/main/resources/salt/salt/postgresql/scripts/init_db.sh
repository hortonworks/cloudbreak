#!/usr/bin/env bash

DATADIR=$(psql -c "show data_directory;" | grep "/data" | xargs)
echo "Datadir: $DATADIR"

if [ ! -f "$DATADIR/init_${SERVICE}_db_executed" ]; then
    set -e
    echo "Create ${SERVICE} database"
    echo "CREATE DATABASE $DATABASE;" | psql -U postgres -v "ON_ERROR_STOP=1"
    echo "CREATE USER $DBUSER WITH PASSWORD '$PASSWORD';" | psql -U postgres -v "ON_ERROR_STOP=1"
    echo "GRANT ALL PRIVILEGES ON DATABASE $DBUSER TO $DATABASE;" | psql -U postgres -v "ON_ERROR_STOP=1"

    echo "Add access to pg_hba.conf"
    echo "host $DATABASE $DBUSER 0.0.0.0/0 md5" >> $DATADIR/pg_hba.conf
    echo "local $DATABASE $DBUSER md5" >> $DATADIR/pg_hba.conf
    echo $(date +%Y-%m-%d:%H:%M:%S) >> $DATADIR/init_${SERVICE}_db_executed
    set +e
fi

