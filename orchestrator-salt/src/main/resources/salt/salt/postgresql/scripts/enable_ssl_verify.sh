#!/usr/bin/env bash
set -e

CONFIG_DIR=$(psql -c "show config_file;" -t | sed 's/\/postgresql.conf//g' | xargs)
echo "CONFIG_DIR: $CONFIG_DIR"

{%- from 'postgresql/settings.sls' import postgresql with context %}
{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE="{{ postgresql.ssl_verification_mode }}"

sed -i 's/^host\([[:space:]]\+.*\)/hostssl\1/g' $CONFIG_DIR/pg_hba.conf
{%- endif %}

{% for service, values in pillar.get('postgres', {}).items()  %}

echo "Add access to pg_hba.conf"
{% if postgresql.ssl_enabled == True %}
sed -i '1ihostssl {{ values['database'] }} {{ values['user'] }} 0.0.0.0/0 md5' $CONFIG_DIR/pg_hba.conf
{%- endif %}


{% endfor %}

{% if postgresql.ssl_enabled == True %}
sed -i '1ihostnossl all all ::0/0 reject' $CONFIG_DIR/pg_hba.conf
sed -i '1ihostnossl all all 0.0.0.0/0 reject' $CONFIG_DIR/pg_hba.conf
{%- endif %}

// TODO postgres restart
set +e
