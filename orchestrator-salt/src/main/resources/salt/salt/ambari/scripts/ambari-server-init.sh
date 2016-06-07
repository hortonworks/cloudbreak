#!/bin/bash

set -x

: ${JAVA_HOME:="/usr/lib/jvm/java"}


wait_for_db() {
  while : ; do
    PGPASSWORD=bigdata psql -h $POSTGRES_DB -U ambari -c "select 1"
    [[ $? == 0 ]] && break
    sleep 5
  done
}

config_remote_jdbc() {
  if [ -z "$POSTGRES_DB" ]
  then
    ambari-server setup --silent --java-home $JAVA_HOME
  else
    echo Configure remote jdbc connection
    ambari-server setup --silent --java-home $JAVA_HOME --database postgres --databasehost $POSTGRES_DB --databaseport 5432 --databasename postgres \
         --postgresschema postgres --databaseusername ambari --databasepassword bigdata
    wait_for_db
    PGPASSWORD=bigdata psql -h $POSTGRES_DB -U ambari postgres < /var/lib/ambari-server/resources/Ambari-DDL-Postgres-CREATE.sql
  fi
}

# https://issues.apache.org/jira/browse/AMBARI-14627
silent_security_setup() {
  ambari-server setup-security --security-option=encrypt-passwords --master-key=bigdata --master-key-persist=true
}

main() {
  # consul-register-service ambari-server $(ip addr show eth0 | grep "inet\b" | awk '{print $2}' | cut -d/ -f1)
  if [ ! -f "/var/ambari-init-executed" ]; then
    config_remote_jdbc
    silent_security_setup
  fi
  echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/ambari-init-executed
}

main "$@"