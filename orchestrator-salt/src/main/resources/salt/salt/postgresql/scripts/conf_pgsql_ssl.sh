#!/usr/bin/env bash

CONFIG_FILE=$(psql -c "show config_file;" -t | xargs)
echo "Config file: $CONFIG_FILE"

set -e
if grep -qR "^ssl =" $CONFIG_FILE; then
    echo "Updating ssl config in the postgresql.conf"
    sed -i.orig "/^ssl =/c\ssl = on" $CONFIG_FILE
else
    echo "Adding ssl config to the postgresql.conf"
    echo "ssl = on" >> $CONFIG_FILE
fi

if grep -qR "^ssl_cert_file =" $CONFIG_FILE; then
    echo "Updating ssl_cert_file config in the postgresql.conf"
    sed -i.orig "/^ssl_cert_file =/c\ssl_cert_file = '{{ postgres_directory }}/certs/postgres.cert'" $CONFIG_FILE
else
    echo "Adding ssl_cert_file config to the postgresql.conf"
    echo "ssl_cert_file = '{{ postgres_directory }}/certs/postgres.cert'" >> $CONFIG_FILE
fi

if grep -qR "^ssl_key_file =" $CONFIG_FILE; then
    echo "Updating ssl_key_file config in the postgresql.conf"
    sed -i.orig "/^ssl_key_file =/c\ssl_key_file = '{{ postgres_directory }}/certs/postgres.key'" $CONFIG_FILE
else
    echo "Adding ssl_key_file config to the postgresql.conf"
    echo "ssl_key_file = '{{ postgres_directory }}/certs/postgres.key'" >> $CONFIG_FILE
fi
set +e