#!/usr/bin/env bash
set -e

CONFIG_DIR=$(psql -c "show config_file;" -t | sed 's/\/postgresql.conf//g' | xargs)
echo "CONFIG_DIR: $CONFIG_DIR"

{% for service, values in pillar.get('postgres', {}).items()  %}

{% if values['user'] is defined %}

echo "Create {{ service }} database"
echo "CREATE DATABASE {{ values['database'] }};" | psql -U postgres -v "ON_ERROR_STOP=1"
echo "CREATE USER {{ values['user'] }} WITH PASSWORD '{{ values['password'] }}';" | psql -U postgres -v "ON_ERROR_STOP=1"
echo "GRANT ALL PRIVILEGES ON DATABASE {{ values['database'] }} TO {{ values['user'] }};" | psql -U postgres -v "ON_ERROR_STOP=1"
echo "ALTER SCHEMA public OWNER TO {{ values['user'] }};" | psql -U postgres -d {{ values['database'] }} -v "ON_ERROR_STOP=1"

echo "Add access to pg_hba.conf"
sed -i '1ihost {{ values['database'] }} {{ values['user'] }} 0.0.0.0/0 md5' $CONFIG_DIR/pg_hba.conf
sed -i '1ilocal {{ values['database'] }} {{ values['user'] }} md5' $CONFIG_DIR/pg_hba.conf
echo $(date +%Y-%m-%d:%H:%M:%S) >> $CONFIG_DIR/init_{{ service }}_db_executed

{% endif %}

{% endfor %}

set +e
