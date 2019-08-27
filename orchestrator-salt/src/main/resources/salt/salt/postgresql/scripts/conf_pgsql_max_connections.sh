#!/usr/bin/env bash

CONFIG_FILE=$(psql -c "show config_file;" -t | xargs)
echo "Config file: $CONFIG_FILE"

set -e
if grep -qR "^max_connections =" $CONFIG_FILE; then
    echo "Updating max_connections config in the postgresql.conf"
    sed -i.orig "/^max_connections =/c\max_connections = 500" $CONFIG_FILE
else
    echo "Adding max_connections config to the postgresql.conf"
    echo "max_connections = 500" >> $CONFIG_FILE
fi
set +e