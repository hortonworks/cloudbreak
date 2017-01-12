#!/usr/bin/env bash

#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

# The app's label
APP_LABEL=Gateway

# The app's name
APP_NAME=gateway

# Start/stop script location
APP_BIN_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Setup the common environment
. $APP_BIN_DIR/knox-env.sh

# The app's jar name
APP_JAR="$APP_BIN_DIR/gateway.jar"

# The apps home dir
APP_HOME_DIR=`dirname $APP_BIN_DIR`

# The apps home dir
APP_CONF_DIR="$APP_HOME_DIR/conf"

# The apps data dir
APP_DATA_DIR="$APP_HOME_DIR/data"

# The app's log dir
APP_LOG_DIR="$APP_HOME_DIR/logs"

# The app's logging options
APP_LOG_OPTS=""

# The app's memory options
APP_MEM_OPTS=""

# The app's debugging options
APP_DBG_OPTS=""

# The app's PID
APP_PID=0

# Start, stop, status, clean or setup
APP_LAUNCH_CMD=$1

# Name of PID file
[[ $ENV_PID_DIR ]] && APP_PID_DIR="$ENV_PID_DIR" || APP_PID_DIR="$APP_HOME_DIR/pids"
APP_PID_FILE="$APP_PID_DIR/$APP_NAME.pid"

# Name of LOG/OUT/ERR file
APP_OUT_FILE="$APP_LOG_DIR/$APP_NAME.out"
APP_ERR_FILE="$APP_LOG_DIR/$APP_NAME.err"

# The start wait time
APP_START_WAIT_TIME=2

# The kill wait time limit
APP_KILL_WAIT_TIME=10

function main {
   case "$1" in
      setup)
         setupEnv
         ;;
      start)
         appStart
         ;;
      stop)
         appStop
         ;;
      status)
         appStatus
         ;;
      clean)
         appClean
         ;;
      help)
         printHelp
         ;;
      *)
         printf "Usage: $0 {start|stop|status|clean}\n"
         ;;
   esac
}

function setupEnv {
   checkEnv
   $JAVA -jar $APP_JAR -persist-master -nostart
   return 0
}

function appStart {
   checkEnv

#   getPID
#   if [ "$?" -eq "0" ]; then
#     printf "$APP_LABEL is already running with PID $APP_PID.\n"
#     exit 0
#   fi
#
#   printf "Starting $APP_LABEL "
#
#   rm -f $APP_PID_FILE
   echo $JAVA $APP_MEM_OPTS $APP_DBG_OPTS $APP_LOG_OPTS -jar $APP_JAR >> $APP_OUT_FILE
   $JAVA $APP_MEM_OPTS $APP_DBG_OPTS $APP_LOG_OPTS -jar $APP_JAR >>$APP_OUT_FILE 2>>$APP_ERR_FILE
#
#   nohup $JAVA $APP_MEM_OPTS $APP_DBG_OPTS $APP_LOG_OPTS -jar $APP_JAR >>$APP_OUT_FILE 2>>$APP_ERR_FILE & printf $!>$APP_PID_FILE || exit 1
#
#   getPID
#   for ((i=0; i<APP_START_WAIT_TIME*10; i++)); do
#      appIsRunning $APP_PID
#      if [ "$?" -eq "0" ]; then break; fi
#      sleep 0.1
#   done
#   appIsRunning $APP_PID
#   if [ "$?" -ne "1" ]; then
#      printf "failed.\n"
#      rm -f $APP_PID_FILE
#      exit 1
#   fi
#   printf "succeeded with PID $APP_PID.\n"
#   return 0
}

function appStop {
   getPID
   appIsRunning $APP_PID
   if [ "$?" -eq "0" ]; then
     printf "$APP_LABEL is not running.\n"
     rm -f $APP_PID_FILE
     return 0
   fi

   printf "Stopping $APP_LABEL with PID $APP_PID "
   appKill $APP_PID >>$APP_OUT_FILE 2>>$APP_ERR_FILE

   if [ "$?" -ne "0" ]; then
     printf "failed. \n"
     exit 1
   else
     rm -f $APP_PID_FILE
     printf "succeeded.\n"
     return 0
   fi
}

function appStatus {
   printf "$APP_LABEL "
   getPID
   if [ "$?" -eq "1" ]; then
     printf "is not running. No PID file found.\n"
     return 0
   fi

   appIsRunning $APP_PID
   if [ "$?" -eq "1" ]; then
     printf "is running with PID $APP_PID.\n"
     exit 1
   else
     printf "is not running.\n"
     return 0
   fi
}

# Removed the app PID file if app is not run
function appClean {
   getPID
   appIsRunning $APP_PID
   if [ "$?" -eq "0" ]; then
     deleteLogFiles
     return 0
   else
     printf "Can't clean files.  $APP_LABEL is running with PID $APP_PID.\n"
     exit 1
   fi
}

function appKill {
   local localPID=$1
   kill $localPID || return 1
   for ((i=0; i<APP_KILL_WAIT_TIME*10; i++)); do
      appIsRunning $localPID
      if [ "$?" -eq "0" ]; then return 0; fi
      sleep 0.1
   done

   kill -s KILL $localPID || return 1
   for ((i=0; i<APP_KILL_WAIT_TIME*10; i++)); do
      appIsRunning $localPID
      if [ "$?" -eq "0" ]; then return 0; fi
      sleep 0.1
   done

   return 1
}

# Returns 0 if the app is running and sets the $PID variable.
function getPID {
   if [ ! -d $APP_PID_DIR ]; then
      printf "Can't find PID dir.\n"
      exit 1
   fi
   if [ ! -f $APP_PID_FILE ]; then
     APP_PID=0
     return 1
   fi

   APP_PID="$(<$APP_PID_FILE)"

   ps -p $APP_PID > /dev/null
   # if the exit code was 1 then it isn't running
   # and it is safe to start
   if [ "$?" -eq "1" ];
   then
     return 1
   fi

   return 0
}

function appIsRunning {
   if [ "$1" -eq "0" ]; then return 0; fi

   ps -p $1 > /dev/null

   if [ "$?" -eq "1" ]; then
     return 0
   else
     return 1
   fi
}

function checkReadDir {
    if [ ! -e "$1" ]; then
        printf "Directory $1 does not exist.\n"
        exit 1
    fi
    if [ ! -d "$1" ]; then
        printf "File $1 is not a directory.\n"
        exit 1
    fi
    if [ ! -r "$1" ]; then
        printf "Directory $1 is not readable by current user $USER.\n"
        exit 1
    fi
    if [ ! -x "$1" ]; then
        printf "Directory $1 is not executable by current user $USER.\n"
        exit 1
    fi
}

function checkWriteDir {
    checkReadDir $1
    if [ ! -w "$1" ]; then
        printf "Directory $1 is not writable by current user $USER.\n"
        exit 1
    fi
}

function checkEnv {
    # Make sure not running as root
    if [ "`id -u`" -eq "0" ]; then
        echo "This command $0 must not be run as root."
        exit 1
    fi
    checkReadDir $APP_CONF_DIR
    checkWriteDir $APP_DATA_DIR
    checkWriteDir $APP_LOG_DIR
    checkWriteDir $APP_PID_DIR
}

function deleteLogFiles {
     rm -f $APP_PID_FILE
     printf "Removed the $APP_LABEL PID file: $APP_PID_FILE.\n"

     rm -f $APP_OUT_FILE
     printf "Removed the $APP_LABEL OUT file: $APP_OUT_FILE.\n"

     rm -f $APP_ERR_FILE
     printf "Removed the $APP_LABEL ERR file: $APP_ERR_FILE.\n"
}

function printHelp {
   $JAVA -jar $APP_JAR -help
   return 0
}

#Starting main
main $APP_LAUNCH_CMD