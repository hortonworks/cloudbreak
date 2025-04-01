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
    # If we got an authentication error, maybe our cookie was somehow bad as well,
    # so make sure we get a new one on the next attempt.
    rm -f $WEBHDFS_COOKIE_JAR
}

webhdfs_command()   {
    local method=$1
    local path=$2
    local query=$3
    exec {out_fd}>&1
    echo "webhdfs_command HTTP response:"
    local http_code=$(curl -k --cookie-jar $WEBHDFS_COOKIE_JAR --cookie $WEBHDFS_COOKIE_JAR \
      -o >(cat >&${out_fd}) \
      --silent \
      --noproxy '*' \
      -w "%{http_code}" \
      -X $method --negotiate -u : \
      "$WEBHDFS_URL$path?$query")
    exec {out_fd}>&-
    echo "webhdfs_command HTTP status code: ${http_code}"
    if [[ "$http_code" -eq 200 ]]; then
      chmod 600 $WEBHDFS_COOKIE_JAR
      return 0
    elif [[ "$http_code" -eq 401 ]]; then
      return 2
    else
      return 1
    fi
}

DELAY=$(( RANDOM % 180 ))
### BEGIN OF SCRIPT ###
echo "Start time of script $0, PID $$: `date` Delay: $DELAY"

# Simplest example is avoiding running multiple instances of script.
exlock_now || exit 1

# Random sleep to avoid all clusters trying to reach FreeIPA at the same time
sleep $DELAY

# the file where all known users yet would be stored. We don't want to unnecessarily invoke namenode ops
# for users we already created dirs for
EXISTING_USERS_FILE=/tmp/existing_users_file
HDFS_KEYTAB=/run/cloudera-scm-agent/process/$(ls -t /run/cloudera-scm-agent/process/ | grep hdfs-NAMENODE$ | head -n 1)/hdfs.keytab
PATH_PREFIX="/user"

export KRB5CCNAME=/tmp/krb5cc_cloudbreak_$EUID


klist -s
if [[ $? -ne 0 ]]; then
  kinit_as_hdfs
fi

WEBHDFS_HTTP_POLICY=$(hdfs getconf -confkey dfs.http.policy)

NAMESERVICE=$(hdfs getconf -confKey dfs.internal.nameservices)
if [ -n "${NAMESERVICE}" ]; then
  echo "Using nameservice $NAMESERVICE"
  NS_FLAG="-ns ${NAMESERVICE}"
fi

if ! hdfs haadmin $NS_FLAG -getAllServiceState
then
  echo "Single node configuration"
  if [ "$WEBHDFS_HTTP_POLICY" == "HTTP_ONLY" ]; then
    NAMENODE_HTTP_ADDRESS=$(hdfs getconf -confkey dfs.namenode.http-address)
    WEBHDFS_URL=http://$NAMENODE_HTTP_ADDRESS/webhdfs/v1
  else
    NAMENODE_HTTP_ADDRESS=$(hdfs getconf -confkey dfs.namenode.https-address)
    WEBHDFS_URL=https://$NAMENODE_HTTP_ADDRESS/webhdfs/v1
  fi
else
  echo "HA configuration"
  NN_HOST=$(hdfs haadmin $NS_FLAG -getAllServiceState | grep active | cut -d':' -f 1)
  echo "Active NAMENODE host: $NN_HOST"
  if [ "$NN_HOST" != "$(hostname -f)" ]; then
    echo "This is not the active host. Script will run on the active one"
    exit 0
  fi
  if [ "$WEBHDFS_HTTP_POLICY" == "HTTP_ONLY" ]; then
    NAMENODE_PORT=$(hdfs getconf -confkey dfs.namenode.http-address | cut -d':' -f 2)
    echo "NN port: $NAMENODE_PORT"
    WEBHDFS_URL=http://$NN_HOST:$NAMENODE_PORT/webhdfs/v1
  else
    NAMENODE_PORT=$(hdfs getconf -confkey dfs.namenode.https-address |cut -d':' -f 2)
    echo "NN port: $NAMENODE_PORT"
    WEBHDFS_URL=https://$NN_HOST:$NAMENODE_PORT/webhdfs/v1
  fi
fi

echo "Webhdfs url: $WEBHDFS_URL"

WEBHDFS_COOKIE_JAR=/tmp/cloudbreak-webhdfs.cookies

case "{{ ldap.directoryType }}" in
 LDAP)              mapfile -t users < <((ipa user-find --pkey-only --sizelimit=0 --timelimit=0) | grep 'User login:' | awk '{ print $3}');;
 ACTIVE_DIRECTORY)  mapfile -t users < <((ldapsearch -x -D "{{ ldap.bindDn }}" -w "{{ ldap.bindPassword }}" -H {{ ldap.connectionURL }} -b "{{ ldap.userSearchBase }}" "(&(objectClass=user)(|(memberOf=CN={{ ldap.adminGroup }},{{ ldap.groupSearchBase }})(memberOf=CN={{ ldap.userGroup }},{{ ldap.groupSearchBase }})))") | grep sAMAccountName | awk '{ print $2}');;
esac

# Hive with remote HMS workaround - TODO remove after OPSAPS-73356
# NOTE: due to */5 cron and initial delay of this script, Hive service may fail with unexpected exits for a few minutes after cluster provision
{% if hiveWithRemoteHiveMetastore %}
users+=('hive')
{% endif %}

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
    # Use WebHDFS to do the 'chown' commands, since the HDFS CLI doesn't offer a batch chown, and webhdfs
    # is hundreds or thousands of times faster than launching a JVM.
    if [[ ${hasharr[$user]} != "exist" ]]; then
      chown_output=$(webhdfs_command PUT $PATH_PREFIX/$user "op=SETOWNER&owner=$user&group=$user")
      chown_status=$?
      if [[ $chown_status -eq 2 ]]; then
        echo 'Negotiation failed, retrying after a kinit...'
        kinit_as_hdfs
        chown_output=$(webhdfs_command PUT $PATH_PREFIX/$user "op=SETOWNER&owner=$user&group=$user")
        chown_status=$?
      fi
      if [[ $chown_status -ne 0 ]]; then
        echo "chown failed for user home dirs even after a kinit as hdfs user.."
        echo $chown_output
        exit 1
      else
        echo "chown'ed the home dir $PATH_PREFIX/$user"
      fi
      echo "$user" >> $EXISTING_USERS_FILE
      number_of_dirs=$((number_of_dirs + 1))
    fi
  done
fi
echo "End time of script $0, PID $$: `date`. Number of HDFS home dirs created/touched $number_of_dirs."
