set_location(){
  echo "setting set_location as"
  echo $1
  sudo sed -i -e "s,object_storage_url:.*,object_storage_url: $1," /srv/pillar/postgresql/disaster_recovery.sls
}

set_db() {
  echo "setting database as"
  echo $1
  sudo sed -i -e "s,database_name:.*,database_name: $1," /srv/pillar/postgresql/disaster_recovery.sls
}

enable_virt_env_and_do_backup() {
  echo "enabling virtual environment for backup"
  env=$(ls /opt/ 2>/dev/null | grep salt_)
  echo $env
  source /opt/$env/bin/activate && salt $(hostname -f) state.apply postgresql.disaster_recovery.backup
}

enable_virt_env_and_do_restore() {
  echo "enabling virtual environment for restore"
  env=$(ls /opt/ 2>/dev/null | grep salt_)
  echo $env
  source /opt/$env/bin/activate && salt $(hostname -f) state.apply postgresql.disaster_recovery.restore
}

get_all_db(){
   all_dbs=$(sudo cat /srv/pillar/postgresql/postgre.sls 2>/dev/null | grep -w database| awk '{print $2}' | sed 's/"//g' | sed 's/,/ /g' | tr -d '\n')
   echo $all_dbs
}

#########################
# The command line help #
#########################
display_help() {
    echo "Usage: sudo bash $0 [(-b or -r)] <path> <database name space seperated> " >&2
    echo
    echo "   --backup_all          perform the backup of all databases to the provided backup path [databases list is not required]"
    echo "   --restore_all         perform the restore of all databases from the provided backup path [databases list is not required]"
    echo "   -b, --backup          perform the backup of the given databases to the provided backup path"
    echo "   -r, --restore         restore the latest backup of the given databases from the provided backup path"
    echo "   backup_path           can be any cloud path where we support backup and restore"
    echo "   database_list         list of databases needs to be backup or restored [space seperated]"
    echo "   sample command for backup   sudo bash backup_restore.sh -b <s3 or abfs path> oozie hue"
    echo "   sample command for restore   sudo bash backup_restore.sh -r <s3 or abfs path> oozie hue"
    echo "   sample command for backup all databases   sudo bash backup_restore.sh --backup_all <s3 or abfs path>"
    echo "   sample command for restore all databases  sudo bash backup_restore.sh --restore_all <s3 or abfs path>"
    echo
    exit 1
}

################################
# Check if parameters options  #
# are given on the commandline #
################################

  case "$1" in
     --backup_all)
        if [ $# -eq 2 ]; then
          shift 1
          set_location $1
          get_all_db
          for w in $all_dbs
          do
                set_db $w
                enable_virt_env_and_do_backup
          done
        else
           display_help
        fi
        ;;
    -b | --backup)
        if [ $# -ge 3 ]; then
          shift 1
          set_location $1
          shift 1
          while [ $# -gt 0 ]
            do
                set_db $1
                enable_virt_env_and_do_backup
                shift 1
            done
        else
           display_help
        fi
        ;;
    -h | --help)
        display_help
        exit 0
        ;;
    -r | --restore)
         if [ $# -ge 3 ]; then
          shift 1
          set_location $1
          shift 1
          while [ $# -gt 0 ]
            do
                set_db $1
                enable_virt_env_and_do_restore
                shift 1
            done
          else
           display_help
        fi
         ;;
    --restore_all)
        if [ $# -eq 2 ]; then
          shift 1
          set_location $1
          get_all_db
          for w in $all_dbs
          do
                set_db $w
                enable_virt_env_and_do_restore
          done
        else
           display_help
        fi
        ;;
    *)
        echo "Error: Unknown option: $1" >&2
        display_help
        exit 1
        ;;
  esac
