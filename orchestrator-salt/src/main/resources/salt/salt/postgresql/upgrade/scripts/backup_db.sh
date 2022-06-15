#!/usr/bin/env bash
# backup_db.sh
# This script is needed as Azure does not support in place RDS upgrade
# This script:
#  - creates backup local temp directory
#  - dumps the global objects and roles to a SQL file with `psql`
#  - for each service:
#     - backs up service data in a parallel manner with `pg_dump` to the temp directory

LOGFILE={{salt['pillar.get']('upgrade:backup:logfile')}}
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

{%- from 'postgresql/settings.sls' import postgresql with context %}
{% if postgresql.ssl_enabled == True %}
export PGSSLROOTCERT="{{ postgresql.root_certs_file }}"
export PGSSLMODE=verify-full
{%- endif %}

dump_global_objects() {
  GLOBAL_OBJECT_BACKUP=${DATE_DIR}/roles.sql
  doLog "INFO Dumping the global objects from database to ${GLOBAL_OBJECT_BACKUP}"
  pg_dumpall -r --host="$HOST" --port="$PORT" --username="$USERNAME" --database=postgres > $GLOBAL_OBJECT_BACKUP 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to dump global objects"
}

backup_database_for_service() {
  SERVICE="$1"

  is_database_exists $SERVICE
  if [ "$?" -eq 1 ];then
    doLog "WARN database for $SERVICE doesn't exists"
    return 0
  fi

  limit_incomming_connection $SERVICE 0
  close_existing_connections $SERVICE

  doLog "INFO Dumping ${SERVICE} ..."

  LOCAL_BACKUP=${DATE_DIR}/${SERVICE}_backup
  pg_dump -Fd -v  --host="$HOST" --port="$PORT" --username="$USERNAME" --dbname=$SERVICE -j 8 -f $LOCAL_BACKUP > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to dump ${SERVICE}"

  doLog "INFO ${SERVICE} dumped to ${LOCAL_BACKUP} finished"

  limit_incomming_connection $SERVICE -1

}

run_backup() {
  BACKUPS_DIR="{{salt['pillar.get']('upgrade:backup:directory')}}"
  [ -z "$BACKUPS_DIR" ] && errorExit "BACKUPS_DIR variable is not defined, check the pillar values!"
  rm -rfv ${BACKUPS_DIR}/* > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2)
  DATE_DIR=${BACKUPS_DIR}/$(date '+%Y-%m-%d_%H:%M:%S')
  mkdir -p "$DATE_DIR" || error_exit "Could not create local directory for backups."

  dump_global_objects
  {% for service, values in pillar.get('postgres', {}).items()  %}
  {% if values['user'] is defined %}
  backup_database_for_service {{ service }}
  {% endif %}
  {% endfor %}

}

doLog "INFO Starting backup"
run_backup
doLog "INFO Completed backup"