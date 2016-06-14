#!/bin/bash -e

: ${LOGFILE:=/var/log/recipes/hdfs-home.log}

create_user_home() {
  export DOMAIN=$(dnsdomainname)
su hdfs<<EOF
  if [ -d /etc/security/keytabs ]; then
    echo "kinit using realm: ${DOMAIN^^}"
  	kinit -V -kt /etc/security/keytabs/dn.service.keytab dn/$(hostname -f)@${DOMAIN^^}
  fi

  if ! hadoop fs -ls /user/$1 2> /dev/null; then
    hadoop fs -mkdir /user/$1 2> /dev/null
    hadoop fs -chown $1:hdfs /user/$1 2> /dev/null
    hadoop dfsadmin -refreshUserToGroupsMappings
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
