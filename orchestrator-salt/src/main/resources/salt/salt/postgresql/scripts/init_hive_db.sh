#!/usr/bin/env bash

echo "PostgreSQL initdb"
postgresql-setup initdb

echo "Create Hive database"
echo "CREATE DATABASE $DATABASE;" | psql -U postgres
echo "CREATE USER $USER WITH PASSWORD '$PASSWORD';" | psql -U postgres
echo "GRANT ALL PRIVILEGES ON DATABASE $USER TO $DATABASE;" | psql -U postgres

echo "Add access to pg_hba.conf"
echo "host $DATABASE $USER 0.0.0.0/0 md5" >> /var/lib/pgsql/data/pg_hba.conf