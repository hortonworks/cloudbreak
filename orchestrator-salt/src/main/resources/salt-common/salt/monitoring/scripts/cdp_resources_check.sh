#!/bin/sh

LOG_FOLDER="/var/log/cdp_resources_check"
LOG_FILE="$LOG_FOLDER/cdp-resources-check.log"
MAX_LOG_FILE_SIZE=10485760

init_logfolder() {
  if [[ ! -d $LOG_FOLDER ]]; then
    mkdir -p $LOG_FOLDER
    touch $LOG_FILE
  fi
  logrotate_if_needed
}

logrotate_if_needed() {
  file_size=`du -b $LOG_FILE | tr -s '\t' ' ' | cut -d' ' -f1`
  if [ $file_size -gt $MAX_LOG_FILE_SIZE ]; then
    timestamp=`date +%s`
    mv $LOG_FILE $LOG_FILE-$timestamp
    touch $LOG_FILE
    ls -1tr $LOG_FILE-* | head -n -20 | xargs --no-run-if-empty rm
  fi
}

collect_system_resources() {
  init_logfolder
  timestamp=`date`
  echo "---------------------" >> $LOG_FILE
  echo "Start system resources checks at $timestamp" >> $LOG_FILE
  echo "---------------------" >> $LOG_FILE
  echo "free -h" >> $LOG_FILE
  echo "---------------------" >> $LOG_FILE
  free -h >> $LOG_FILE
  echo "---------------------" >> $LOG_FILE
  echo "df -h" >> $LOG_FILE
  echo "---------------------" >> $LOG_FILE
  df -h >> $LOG_FILE
  echo "---------------------" >> $LOG_FILE
  echo "ps -eo pid,ppid,cmd,%mem,%cpu --sort=-%mem | head" >> $LOG_FILE
  echo "---------------------" >> $LOG_FILE
  ps -eo pid,ppid,%mem,%cpu,cmd --sort=-%mem | head >> $LOG_FILE
  echo "---------------------" >> $LOG_FILE
  echo "du -a -h --max-depth=1 /var/log | sort -r -h | head -10" >> $LOG_FILE
  echo "---------------------" >> $LOG_FILE
  du -a -h --max-depth=1 /var/log | sort -r -h | head -10 >> $LOG_FILE
  echo "---------------------" >> $LOG_FILE
  echo "sysctl fs.file-nr" >> $LOG_FILE
  sysctl fs.file-nr >> $LOG_FILE
  echo "---------------------" >> $LOG_FILE
  echo "sysctl fs.file-max" >> $LOG_FILE
  sysctl fs.file-max >> $LOG_FILE
  echo "---------------------" >> $LOG_FILE
  echo "ls -la /proc/$$/fd" >> $LOG_FILE
  ls -la /proc/$$/fd >> $LOG_FILE
}

collect_system_resources