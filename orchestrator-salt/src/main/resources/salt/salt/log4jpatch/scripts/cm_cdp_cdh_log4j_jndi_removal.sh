#!/bin/bash
# CLOUDERA SCRIPTS FOR LOG4J
#
# (C) Cloudera, Inc. 2021. All rights reserved.
#
# Applicable Open Source License: Apache License 2.0
#
# CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND. CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. CLOUDERA IS NOT LIABLE TO YOU,  AND WILL NOT DEFEND, INDEMNIFY, NOR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING FROM OR RELATED TO THE CODE. ND WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR ONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED TO, DAMAGES  RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF  BUSINESS ADVANTAGE OR UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
#
# --------------------------------------------------------------------------------------

function scan_for_jndi {
  if ! command -v zipgrep &> /dev/null; then
    echo "zipgrep not found. zipgrep is required to run this script."
    exit 1
  fi

  if ! command -v zgrep &> /dev/null; then
    echo "zgrep not found. zgrep is required to run this script."
    exit 1
  fi

  targetdir=${1:-/opt/cloudera}
  echo "Running on '$targetdir'"

  pattern=JndiLookup.class

  shopt -s globstar

  for jarfile in $targetdir/**/*.{jar,tar}; do
    if grep -q $pattern $jarfile; then
      # Vulnerable class/es found
      echo "Vulnerable class: JndiLookup.class found in '$jarfile'"
    fi
  done

  for warfile in $targetdir/**/*.war; do
    if zipgrep -q $pattern $warfile; then
      # Vulnerable class/es found
      echo "Vulnerable class: JndiLookup.class found in '$warfile'"
    fi
  done

  for tarfile in $targetdir/**/*.{tar.gz,tgz}; do
    if zgrep -q $pattern $tarfile; then
      # Vulnerable class/es found
      echo "Vulnerable class: JndiLookup.class found in '$tarfile'"
    fi
  done

  echo "Scan complete"

}


function delete_jndi_from_jar_files {

  if ! command -v zip &> /dev/null; then
    echo "zip not found. zip is required to run this script."
    exit 1
  fi

  targetdir=${1:-/opt/cloudera}
  echo "Running on '$targetdir'"

  backupdir=${2:-/opt/cloudera/log4shell-backup}
  mkdir -p "$backupdir"
  echo "Backing up files to '$backupdir'"

  shopt -s globstar
  for jarfile in $targetdir/**/*.jar; do
    if grep -q JndiLookup.class $jarfile; then
      # Backup file only if backup doesn't already exist
      mkdir -p "$backupdir/$(dirname $jarfile)"
      targetbackup="$backupdir/$jarfile.backup"
      if [ ! -f "$targetbackup" ]; then
        echo "Backing up to '$targetbackup'"
        cp -f "$jarfile" "$targetbackup"
      fi

      # Rip out class
      echo "Deleting JndiLookup.class from '$jarfile'"
      zip -q -d "$jarfile" \*/JndiLookup.class
    fi
  done

  echo "Completed removing JNDI from jar files"

}

function delete_jndi_from_targz_file {

  tarfile=$1
  if [ ! -f "$tarfile" ]; then
    echo "Tar file '$tarfile' not found"
    exit 1
  fi

  backupdir=${2:-/opt/cloudera/log4shell-backup}
  mkdir -p "$backupdir/$(dirname $tarfile)"
  targetbackup="$backupdir/$tarfile.backup"
  if [ ! -f "$targetbackup" ]; then
    echo "Backing up to '$targetbackup'"
    cp -f "$tarfile" "$targetbackup"
  fi

  echo "Patching '$tarfile'"
  tempfile=$(mktemp)
  tempdir=$(mktemp -d)
  tempbackupdir=$(mktemp -d)

  tar xf "$tarfile" -C "$tempdir"
  delete_jndi_from_jar_files "$tempdir" "$tempbackupdir"

  echo "Recompressing"
  (cd "$tempdir" && tar czf "$tempfile" --owner=1000 --group=100 .)

  # Restore old permissions before replacing original
  chown --reference="$tarfile" "$tempfile"
  chmod --reference="$tarfile" "$tempfile"

  mv "$tempfile" "$tarfile"

  rm -f $tempfile
  rm -rf $tempdir
  rm -rf $tempbackupdir

  echo "Completed removing JNDI from $tarfile"

}

function delete_jndi_from_hdfs {

  mr_hdfs_path="/user/yarn/mapreduce/mr-framework/"
  tez_hdfs_path="/user/tez/*"
  username=""
  issecure="true"

  if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
        echo "Invalid arguments. Please choose 'mr' or 'tez' along with optional tar ball path."
    exit 1
  fi

  file_type=$1
  external_hdfs_path=${2:-""}

  if [ $file_type ==  "tez" ]; then
    if [ -z "$external_hdfs_path" ]; then
      external_hdfs_path=$tez_hdfs_path
    fi
    hdfs_path=$external_hdfs_path
    username="tez"
  elif [ $file_type == "mr" ]; then
    if [ -z "$external_hdfs_path" ]; then
      external_hdfs_path=$mr_hdfs_path
    fi
    hdfs_path=$external_hdfs_path
    username="yarn"
  else
    echo "Invalid arguments. Please choose 'mr' or 'tez' along with optional tar ball path."
    exit 1
  fi

  keytab_file="hdfs.keytab"
  keytab=$(find /var/run/cloudera-scm-agent/process/ -type f -iname $keytab_file | grep -e NAMENODE -e DATANODE | tail -1)
  if [ -z "$keytab" ]; then
    echo "Keytab file not found: $keytab_file. Considering this as a non-secure cluster deployment."
    issecure="false"
  fi

  if [ $issecure == "true" ]; then
    echo "Using $keytab to access HDFS"

    principal=$(klist -kt $keytab | grep -v HTTP | tail -1 | awk '{print $4}')
    if [ -z "$principal" ]; then
      echo "principal not found: $principal"
      exit 0
    fi
    kinit -kt $keytab $principal
  fi

  # In case two or more namenode host cleanups are running at exactly the same time, stagger the
  # probe of the HDFS marker.
  sleep $((1 + $RANDOM % 20))
  hdfs dfs -test -e $hdfs_path
  ret_status=$?
  if [ $ret_status -eq 1 ]; then
    if [ $file_type ==  "tez" ]; then
      echo "Tar ball is not available in $hdfs_path. Tez is not installed."
      return
    else
      echo "Tar ball is not available in $hdfs_path. Exiting gracefully"
      exit 0
    fi
  fi

  hdfs_file_path=$(hdfs dfs -ls $hdfs_path | tail -1  | awk '{print $8}')

  if [[ ! $hdfs_file_path == *.tar.gz ]]; then
    echo "Desired tar ball path was not found in HDFS. Exiting."
    exit 0
  fi

  hdfs_lock_path="/user/upgrade-lock_${file_type}"
  hdfs dfs -test -e $hdfs_lock_path
  ret_status=$?
  if [ $ret_status -eq 1 ]; then
    hdfs dfs -touch $hdfs_lock_path
  else
    echo "Tar ball for $file_type in HDFS is already upgraded."
    return 0
  fi

  current_time=$(date "+%Y.%m.%d-%H.%M.%S")
  echo "Current Time : $current_time"

  local_path="/tmp/hdfs_tar_files.${current_time}"
  mkdir -p $local_path

  echo "Downloading tar ball from HDFS path $hdfs_file_path to $local_path"
  echo "Printing current HDFS file stats"
  hdfs dfs -ls $hdfs_file_path
  hdfs dfs -get -f $hdfs_file_path $local_path

  hdfs_bc_path="/tmp/backup.${current_time}"

  echo "Taking a backup of HDFS dir $hdfs_file_path to $hdfs_bc_path"
  hdfs dfs -mkdir -p $hdfs_bc_path
  hdfs dfs -cp -f  $hdfs_file_path $hdfs_bc_path

  out="$(basename $local_path/*)"
  local_full_path="${local_path}/${out}"

  echo "Executing the log4j removal script"
  delete_jndi_from_targz_file $local_full_path

  echo "Completed executing log4j removal script and uploading $out to $hdfs_file_path"
  hdfs dfs -copyFromLocal -f $local_full_path $hdfs_file_path
  hdfs dfs -chown $username $hdfs_file_path

  echo "Printing updated HDFS file stats"
  hdfs dfs -ls $hdfs_file_path

  if [ $issecure == "true" ]; then
    which kdestroy && kdestroy
  fi


}

function usage() {
cat << EOF
Search for and remove instances of the log4j security hole within Cloudera artifacts.
OPTIONAL PARAMETERS
target directory: The installed path of Cloudera software, default=/opt/cloudera
backupdir: Where the original jar and tar.gz files will be saved, default /opt/cloudera/log4shell-backup
EOF
}



targetdir=${1:-/opt/cloudera}
backupdir=${2:-/opt/cloudera/log4shell-backup}

if [ -z "$SKIP_JAR" ]; then
  echo "Removing JNDI from jar files"
  delete_jndi_from_jar_files $targetdir $backupdir
else
  echo "Skipped patching .jar"
fi

if [ -z "$SKIP_TGZ" ]; then
  echo "Removing JNDI from tar.gz files"
  for targzfile in $(find $targetdir -name '*.tar.gz') ; do
    delete_jndi_from_targz_file $targzfile $backupdir
  done
else
  echo "Skipped patching .tar.gz"
fi

if [ -z "$SKIP_HDFS" ]; then
  if ps -efww | grep org.apache.hadoop.hdfs.server.namenode.NameNode | grep -v grep  1>/dev/null 2>&1; then
    echo "Found an HDFS namenode on this host, removing JNDI from HDFS tar.gz files"
    delete_jndi_from_hdfs tez
    delete_jndi_from_hdfs mr
  fi
else
  echo "Skipped patching .tar.gz in HDFS"
fi

if [ -n "$RUN_SCAN" ]; then
  echo "Running scan for missed JndiLookup classes. This may take a while."
  scan_for_jndi $targetdir
fi
