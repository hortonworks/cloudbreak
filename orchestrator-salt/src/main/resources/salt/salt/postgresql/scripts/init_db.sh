#!/usr/bin/env bash
set -e

DATADIR=$(psql -c "show data_directory;" | grep "/data" | xargs)
echo "Datadir: $DATADIR"

{% for service, values in pillar.get('postgres', {}).items()  %}

{% if values['user'] is defined %}

echo "Create {{ service }} database"
echo "CREATE DATABASE {{ values['database'] }};" | psql -U postgres -v "ON_ERROR_STOP=1"
echo "CREATE USER {{ values['user'] }} WITH PASSWORD '{{ values['password'] }}';" | psql -U postgres -v "ON_ERROR_STOP=1"
echo "GRANT ALL PRIVILEGES ON DATABASE {{ values['user'] }} TO {{ values['database'] }};" | psql -U postgres -v "ON_ERROR_STOP=1"

echo "Add access to pg_hba.conf"
echo "host {{ values['database'] }} {{ values['user'] }} 0.0.0.0/0 md5" >> $DATADIR/pg_hba.conf
echo "local {{ values['database'] }} {{ values['user'] }} md5" >> $DATADIR/pg_hba.conf
echo $(date +%Y-%m-%d:%H:%M:%S) >> $DATADIR/init_{{ service }}_db_executed

{% endif %}

{% endfor %}

set +e