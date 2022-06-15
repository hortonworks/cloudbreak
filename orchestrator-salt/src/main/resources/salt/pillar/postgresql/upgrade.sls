upgrade:
  backup:
    logfile: /var/log/postgres_upgrade_backup.log
    directory: /var/tmp/postges_upgrade_backup
  restore:
    logfile: /var/log/postgres_upgrade_restore.log