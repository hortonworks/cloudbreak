#!/bin/bash -e

: ${LOGFILE:=/var/log/recipes/hdfs-home.log}

create_user_home(){
su hdfs<<EOF
  if [ -d /etc/security/keytabs ]; then
  	kinit -V -kt /etc/security/keytabs/dn.service.keytab dn/$(hostname -f)@NODE.DC1.CONSUL
  fi

  if ! hadoop fs -ls /user/$1 2> /dev/null; then
    hadoop fs -mkdir /user/$1 2> /dev/null
    hadoop fs -chown $1:hadoop /user/$1 2> /dev/null
    echo "created /user/$1"
  else
    echo "/user/$1 already exists, skipping..."
  fi
EOF
}

main(){
  create_user_home yarn
  create_user_home $USER
}

exec &>> "$LOGFILE"
[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
