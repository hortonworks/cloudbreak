#!/usr/bin/env bash


# This script is for creating hdfs home directories for all the workload cluster users.
# Script gets the list of users from IPA Server. If the user's directory is not present on hdfs then
# it creates one for that user and change ownership also to that user.
# Currently doesn't do anything to do with deletion of users

### HEADER ###

LOCKFILE="/var/lock/`basename $0`"
LOCKFD=99

# PRIVATE
_lock()             { flock -$1 $LOCKFD; }
_no_more_locking()  { _lock u; _lock xn && rm -f $LOCKFILE; }
_prepare_locking()  { eval "exec $LOCKFD>\"$LOCKFILE\""; trap _no_more_locking EXIT; }

# ON START
_prepare_locking

# PUBLIC
exlock_now()        { _lock xn; }  # obtain an exclusive lock immediately or fail
exlock()            { _lock x; }   # obtain an exclusive lock
shlock()            { _lock s; }   # obtain a shared lock
unlock()            { _lock u; }   # drop a lock

### BEGIN OF SCRIPT ###
echo "Start time of script $0, PID $$: `date`"

# Simplest example is avoiding running multiple instances of script.
exlock_now || exit 1

# the file where all known users yet would be stored. We don't want to unnecessarily invoke namenode ops
# for users we already created dirs for
EXISTING_USERS_FILE=/tmp/existing_users_file

HDFS_KEYTAB=/run/cloudera-scm-agent/process/$(ls -t /run/cloudera-scm-agent/process/ | grep hdfs-NAMENODE$ | head -n 1)/hdfs.keytab
echo "Keytab dir:$HDFS_KEYTAB"

PATH_PREFIX="/user"

# Check if there's a valid TGT, and kinit if not
klist -s
if [[ $? -ne 0 ]]; then
  kinit -kt "$HDFS_KEYTAB" hdfs/$(hostname -f)
  if [[ $? -ne 0 ]]; then
    echo "Couldn't kinit as hdfs"
    exit 1
  fi
else
  echo "Found a valid keytab."
fi

mapfile -t users < <((ipa user-find --sizelimit=0 --timelimit=0) | grep 'User login:' | awk '{ print $3}')
declare -a existingusers
if test -f "$EXISTING_USERS_FILE"; then
  mapfile -t existingusers < <(cat $EXISTING_USERS_FILE)
fi
# hashtable for lookup later
declare -A hasharr
for existing in "${existingusers[@]}";
do
  hasharr[$existing]="exist"
done
number_of_dirs=0
newpaths=""
for user in "${users[@]}";
do
  if [[ ${hasharr[$user]} != "exist" ]]; then
     echo "Will attempt to create HDFS home dir for $user"
     #the size of newpaths will be capped by `getconf ARG_MAX` most likely.
     #This is set to 2MB on mow-dev, and so this string concatenation can
     #easily take 10s of 1000s of meaningful paths. TODO: fix this longer
     #term to do checks against the limits, etc.
     newpaths+="$PATH_PREFIX/$user "
  fi
done
if [ -z "$newpaths" ]; then
  echo "No home dirs to create in HDFS"
else
  hdfs dfs -mkdir -p $newpaths
  echo "Will chown the created home directories. This may take a while depending on the number of directories."
  for user in "${users[@]}";
  do
    if [[ ${hasharr[$user]} != "exist" ]]; then
      hdfs dfs -chown $user:$user $PATH_PREFIX/$user
      echo "$user" >> $EXISTING_USERS_FILE
      number_of_dirs=$((number_of_dirs + 1))
    fi
  done
fi
echo "End time of script $0, PID $$: `date`. Number of HDFS home dirs created/touched $number_of_dirs."
