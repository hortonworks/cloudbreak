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

CIPHERS="ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES128-SHA256:ECDHE-ECDSA-AES256-SHA384:DHE-RSA-AES128-SHA256:DHE-RSA-AES256-SHA256"
if grep -qR "^ssl_ciphers =" $CONFIG_FILE; then
    echo "Updating ssl_ciphers config in the postgresql.conf"
    sed -i.orig "/^ssl_ciphers =/c\ssl_ciphers = '$CIPHERS'" $CONFIG_FILE
else
    echo "Adding ssl_ciphers config to the postgresql.conf"
    echo "ssl_ciphers = '$CIPHERS'" >> $CONFIG_FILE
fi
set +e