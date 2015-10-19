#!/bin/bash -e

: ${LOGFILE:=/var/log/consul-watch/consul_handler.log}

create_user_home(){
  if ! su hdfs -c "hadoop fs -ls /user/$1" 2> /dev/null; then
    su hdfs -c "hadoop fs -mkdir /user/$1" 2> /dev/null
    su hdfs -c "hadoop fs -chown $1:hadoop /user/$1" 2> /dev/null
    echo "created /user/$1"
  else
    echo "/user/$1 already exists, skipping..."
  fi
}

main(){
  create_user_home yarn
  create_user_home $USER
}

exec &>> "$LOGFILE"
[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
