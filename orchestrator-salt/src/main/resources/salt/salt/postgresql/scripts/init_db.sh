#!/usr/bin/env bash

DATADIR=$(psql -c "show data_directory;" | grep "/data")
echo "Datadir: $DATADIR"

if [ ! -f "$DATADIR/init_${SERVICE}_db_executed" ]; then
    set -e
    echo "Create ${SERVICE} database"
    echo "CREATE DATABASE $DATABASE;" | psql -U postgres
    echo "CREATE USER $USER WITH PASSWORD '$PASSWORD';" | psql -U postgres
    echo "GRANT ALL PRIVILEGES ON DATABASE $USER TO $DATABASE;" | psql -U postgres

    echo "Add access to pg_hba.conf"
    echo "host $DATABASE $USER 0.0.0.0/0 md5" >> $DATADIR/pg_hba.conf
    echo "local $DATABASE $USER md5" >> $DATADIR/pg_hba.conf
    echo $(date +%Y-%m-%d:%H:%M:%S) >> $DATADIR/init_${SERVICE}_db_executed
    set +e
fi