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
  # This is needed because of https://github.com/MicrosoftDocs/azure-docs/blob/main/articles/postgresql/single-server/how-to-upgrade-using-dump-and-restore.md#migrate-the-roles
  sed -i -e 's/\(NOSUPERUSER\|NOBYPASSRLS\)//g' $GLOBAL_OBJECT_BACKUP
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
  set -x
  LOCAL_BACKUP=${DATE_DIR}/${SERVICE}_backup
  LOCAL_BACKUP_TOC=${DATE_DIR}/${SERVICE}_backup_toc
  pg_dump -Fd -v  --host="$HOST" --port="$PORT" --username="$USERNAME" --dbname=$SERVICE -j 8 -f $LOCAL_BACKUP > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2) || errorExit "Unable to dump ${SERVICE}"
  # These 2 lines are required to handle extensions correctly during Single 10|11 --> Flexible 11|14 migration, see CB-22820 for more context
  pg_restore -l $LOCAL_BACKUP > $LOCAL_BACKUP_TOC
  awk '/EXTENSION|pg_buffercache|pg_stat_statements|plpgsql/ { $0 = ";" $0 }; 1' $LOCAL_BACKUP_TOC >tmp && mv tmp $LOCAL_BACKUP_TOC
  set +x
  doLog "INFO ${SERVICE} dumped to ${LOCAL_BACKUP} finished"

  limit_incomming_connection $SERVICE -1

}

move_backup_to_cloud () {

  BACKUP_LOCATION={{salt['pillar.get']('postgres:upgrade:backup_location')}}
  ABFS_FILE_SYSTEM={{salt['pillar.get']('postgres:upgrade:abfs_file_system')}}
  ABFS_FILE_SYSTEM_FOLDER={{salt['pillar.get']('postgres:upgrade:abfs_file_system_folder')}}
  ABFS_ACCOUNT_NAME={{salt['pillar.get']('postgres:upgrade:abfs_account_name')}}
  CLUSTER_NAME={{salt['pillar.get']('cluster:name')}}
  AZURE_INSTANCE_MSI={{salt['pillar.get']('postgres:upgrade:backup_instance_profile')}}

  [ -z "$BACKUP_LOCATION" ] && doLog "WARN BACKUP_LOCATION variable is not defined, skipping the cloud storage upload!" && return 1

  BACKUPS_DIR="$1"
  LOCAL_BACKUP_FILE_NAME={{salt['pillar.get']('upgrade:backup:compressed_file_name')}}
  doLog "INFO attempting backup upload to: ${BACKUP_LOCATION}, compressing to file ${LOCAL_BACKUP_FILE_NAME}"

  tar -czvf "$LOCAL_BACKUP_FILE_NAME" "${BACKUPS_DIR}"
  doLog "INFO Compressed file size is $(du -h $LOCAL_BACKUP_FILE_NAME)"
  TARGET_LOCATION="${ABFS_FILE_SYSTEM_FOLDER}/rds_backup/${CLUSTER_NAME}/"
  CLOUD_TARGET_LOCATION=https://${ABFS_ACCOUNT_NAME}.blob.core.windows.net/${ABFS_FILE_SYSTEM}${TARGET_LOCATION}

  doLog "INFO Detected ABFS configs: Account=$ABFS_ACCOUNT_NAME, Container=$ABFS_FILE_SYSTEM, BasePath=$ABFS_FILE_SYSTEM_FOLDER, msi: ${AZURE_INSTANCE_MSI}"
  /bin/keyctl new_session 2>&1 | tee -a "${LOGFILE}"
  doLog "INFO setting up keyring"
  if [[ "${PIPESTATUS[0]}" -ne "0" ]]; then
      SESSION_PERMS=$(/bin/keyctl rdescribe @s)
      SESSION_OWNER=$(echo "${SESSION_PERMS}" | cut -f2 -d";")
      if [[ "${SESSION_OWNER}" != "${UID}" ]]; then
          errorExit "Unable to setup keyring session. The keyring session permissions are ${SESSION_PERMS} but the UID is ${UID}. Was this command run with \"sudo\" or \"sudo su\"? If so, then try \"sudo su -\"."
      else
          errorExit "Unable to setup keyring session"
      fi
  fi

  doLog "INFO Logging in with msi ${AZURE_INSTANCE_MSI}"
  /usr/local/bin/azcopy login --identity --identity-resource-id "${AZURE_INSTANCE_MSI}"
  if [[ "${PIPESTATUS[0]}" -ne "0" ]]; then
      errorExit "Unable to login to Azure with msi ${AZURE_INSTANCE_MSI}"
  fi

  doLog "INFO uploading with azcopy ${LOCAL_BACKUP_FILE_NAME} to ${CLOUD_TARGET_LOCATION}"
  /usr/local/bin/azcopy copy "$LOCAL_BACKUP_FILE_NAME" "${CLOUD_TARGET_LOCATION}" --recursive=true --check-length=false
  if [[ "${PIPESTATUS[0]}" -ne "0" ]]; then
      errorExit "ABFS upload FAILED: Account=$ABFS_ACCOUNT_NAME, Container=$ABFS_FILE_SYSTEM, Path=${TARGET_LOCATION}${LOCAL_BACKUP_FILE_NAME}"
  fi

  rm -f ${LOCAL_BACKUP_FILE_NAME}
  doLog "INFO Completed upload to ${BACKUP_LOCATION}"
}

run_backup() {
  BACKUPS_DIR="{{salt['pillar.get']('upgrade:backup:directory')}}"
  [ -z "$BACKUPS_DIR" ] && errorExit "BACKUPS_DIR variable is not defined, check the pillar values!"
  rm -rfv ${BACKUPS_DIR}/* > >(tee -a $LOGFILE) 2> >(tee -a $LOGFILE >&2)
  DATE_DIR=${BACKUPS_DIR}/$(date '+%Y-%m-%d_%H:%M:%S')
  mkdir -p "$DATE_DIR" || errorExit "Could not create local directory for backups."

  dump_global_objects
  {% for service, values in pillar.get('postgres', {}).items()  %}
  {% if values['user'] is defined %}
  backup_database_for_service {{ service }}
  {% endif %}
  {% endfor %}
  move_backup_to_cloud $DATE_DIR
}

doLog "INFO Starting backup"
run_backup
doLog "INFO Completed backup"