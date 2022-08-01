#!/bin/bash
# restore_db.sh
# This script is needed as Azure does not support in place RDS upgrade
# This script:
#  - expects the output of a successful backup, in a specific location
#  - loads the global objects and roles from a SQL file with `psql`,
#     - if the file does not exist then aborts
#     - some errors are expected to happen due to already existing objects in the target database
#  - for each service:
#     - drops and creates the respective database of the service
#     - loads the backup data in a parallel manner with `pg_restore`
#  - if all is successful, then it removes the backup

LOGFILE={{salt['pillar.get']('upgrade:restore:logfile')}}
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

BACKUPS_DIR="{{salt['pillar.get']('upgrade:backup:directory')}}"
[ -z "$BACKUPS_DIR" ] && errorExit "BACKUPS_DIR variable is not defined, check the pillar values!"

{%- from 'postgresql/settings.sls' import postgresql with context %}
{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE=verify-full
{%- endif %}

restore_global_objects() {
  GLOBAL_OBJECT_BACKUP="${BACKUP_DIR}"/roles.sql
  if [ -f "${GLOBAL_OBJECT_BACKUP}" ]; then
    psql -f ${GLOBAL_OBJECT_BACKUP} --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to restore global objects from ${GLOBAL_OBJECT_BACKUP}"
  else
      errorExit "INFO Not restoring global objects as ${GLOBAL_OBJECT_BACKUP} does not exist"
  fi
}

restore_database_for_service() {
    SERVICE="$1"

    BACKUP="${BACKUP_DIR}/${SERVICE}_backup"

    if [ -d "$BACKUP" ]; then
      limit_incomming_connection $SERVICE 0
      close_existing_connections $SERVICE
      doLog "INFO Restoring $SERVICE"
      psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "drop database if exists ${SERVICE} ;" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to drop database ${SERVICE}"
      psql --host="$HOST" --port="$PORT" --dbname="postgres" --username="$USERNAME" -c "create database ${SERVICE};" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to re-create database ${SERVICE}"
      pg_restore -v --host="$HOST" --port="$PORT" --dbname="$SERVICE" --username="$USERNAME" -j 8 ${BACKUP} > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to restore ${SERVICE}"
      doLog "INFO Successfully restored ${SERVICE}"
      limit_incomming_connection $SERVICE -1
  else
      doLog "INFO Not restoring ${SERVICE} as ${BACKUP} does not exist"
  fi
}

run_restore() {

  BACKUP_DIR=$(ls -td $BACKUPS_DIR/*/ | head -1)
  restore_global_objects
  {% for service, values in pillar.get('postgres', {}).items()  %}
  {% if values['user'] is defined %}
  restore_database_for_service {{ service }}
  {% endif %}
  {% endfor %}
  # TODO: Commented only for test cases, uncomment before feature is released
  #rm -rfv "$BACKUPS_DIR/*" > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2)

}

doLog "INFO Initiating restore"
run_restore
doLog "INFO Completed restore."