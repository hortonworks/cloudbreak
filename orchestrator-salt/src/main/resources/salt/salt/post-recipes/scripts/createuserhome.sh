#!/usr/bin/env bash


# This script is for creating hdfs home directories for all the workload cluster users.
# Script gets the list of users from IPA Server. If the user's directory is not present on hdfs then
# it creates one for that user and change ownership also to that user.
# Any hdfs errors seen are assumed as to do with kerberos, and a 'kinit' is attempted after which the 
# hdfs operation is repeated. Since the operation is done as 'hdfs' principal, normally the operation
# shouldn't fail....
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

kinit_as_hdfs()     {
    echo "kinit as hdfs using Keytab: $HDFS_KEYTAB"
    kinit -kt "$HDFS_KEYTAB" hdfs/$(hostname -f)
    if [[ $? -ne 0 ]]; then
      echo "Couldn't kinit as hdfs"
      exit 1
    fi
}

### BEGIN OF SCRIPT ###
echo "Start time of script $0, PID $$: `date`"

# Simplest example is avoiding running multiple instances of script.
exlock_now || exit 1

# the file where all known users yet would be stored. We don't want to unnecessarily invoke namenode ops
# for users we already created dirs for
EXISTING_USERS_FILE=/tmp/existing_users_file
HDFS_KEYTAB=/run/cloudera-scm-agent/process/$(ls -t /run/cloudera-scm-agent/process/ | grep hdfs-NAMENODE$ | head -n 1)/hdfs.keytab
PATH_PREFIX="/user"

klist -s
if [[ $? -ne 0 ]]; then
  kinit_as_hdfs
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
  # Check if there's an error, and kinit if so, and then repeat mkdir
  if [[ $? -ne 0 ]]; then
    kinit_as_hdfs
    hdfs dfs -mkdir -p $newpaths
    if [[ $? -ne 0 ]]; then
      echo "mkdir failed for user home dirs even after a kinit as hdfs user.."
      exit 1
    else
      echo "Created the home dirs.."
    fi
  else
    echo "Created the home dirs.."
  fi
  echo "Will chown the created home directories. This may take a while depending on the number of directories."
  for user in "${users[@]}";
  do
    if [[ ${hasharr[$user]} != "exist" ]]; then
      hdfs dfs -chown $user:$user $PATH_PREFIX/$user
      if [[ $? -ne 0 ]]; then
        kinit_as_hdfs
        hdfs dfs -chown $user:$user $PATH_PREFIX/$user
        if [[ $? -ne 0 ]]; then
          echo "chown failed for user home dirs even after a kinit as hdfs user.."
          exit 1
        else
          echo "chown'ed the home dir $PATH_PREFIX/$user"
        fi
      else
        echo "chown'ed the home dir $PATH_PREFIX/$user"
      fi
      echo "$user" >> $EXISTING_USERS_FILE
      number_of_dirs=$((number_of_dirs + 1))
    fi
  done
fi
echo "End time of script $0, PID $$: `date`. Number of HDFS home dirs created/touched $number_of_dirs."
