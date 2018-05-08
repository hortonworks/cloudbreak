#!/usr/bin/env bash

DATADIR=$(psql -c "show data_directory;" | grep "/data" | xargs)
echo "Datadir: $DATADIR"

set -e
if grep -qR "^listen_addresses =" $DATADIR/postgresql.conf; then
    echo "Updating listen_addresses config in the postgresql.conf"
    sed -i.orig "/^listen_addresses =/c\listen_addresses = '*'" $DATADIR/postgresql.conf
else
    echo "Adding listen_addresses config to the postgresql.conf"
    echo "listen_addresses = '*'" >> $DATADIR/postgresql.conf
fi
set +e