#!/bin/bash
# check_db_connection.sh
# This script is needed because after private single to private flexible db upgrade on Azure the flexible server is not reachable for couple of minutes
# This script:
#  - check the db connectivty with a psql version check command

LOGFILE={{salt['pillar.get']('upgrade:checkconnection:logfile')}}
[ -z "LOGFILE" ] && echo "LOGFILE variable is not defined, check the pillar values!" && exit 1
echo "Logs at ${LOGFILE}"

source /opt/salt/scripts/common_utils.sh

print_usage() {
  echo "Script accepts 3 input parameters:"
  echo "  -h PostgreSQL host name."
  echo "  -p PostgreSQL port."
  echo "  -u PostgreSQL user name."
}

while getopts ":h:p:u:" OPTION; do
    case $OPTION in
    h  )
        HOST="$OPTARG"
        ;;
    p  )
        PORT="$OPTARG"
        ;;
    u  )
        USERNAME="$OPTARG"
        if [ -n "$VERSION" ] && [ $VERSION -eq 14 ] && [ "$IS_REMOTE_DB" != "None" ]; then
          USERNAME=$(cut -d @ -f 1 <<< "$USERNAME")
          doLog "INFO Set USERNAME to $USERNAME, original is $OPTARG"
        fi
        ;;
    \? ) echo "Unknown option: -$OPTARG" >&2;
         print_usage
         exit 1;;
    :  ) echo "Missing option argument for -$OPTARG" >&2; exit 1;;
    esac
done
[ -z "$HOST" ] || [ -z "$PORT" ] || [ -z "$USERNAME" ] && echo "At least one mandatory parameter is not set!" && print_usage && exit 1

set -o nounset
set -o pipefail

exec 3>&1 4>&2
trap 'exec 2>&4 1>&3' 0 1 2 3
exec 1> >(tee -a "${LOGFILE}") 2> >(tee -a "${LOGFILE}" >&2)
exec 5>>"${LOGFILE}"
BASH_XTRACEFD=5

{%- from 'postgresql/settings.sls' import postgresql with context %}
{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE="{{ postgresql.ssl_verification_mode }}"
{%- endif %}

check_connection() {
  set -x
  psql -c "show server_version;" --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to connect to the database"
  set +x
}

doLog "INFO Checking db connection..."
check_connection
doLog "INFO Db connection has been checked."
