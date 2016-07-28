#!/bin/bash

set -x

: ${JAVA_HOME:="/usr/lib/jvm/java"}

config_remote_jdbc() {
    if [[ '{{ ambari_database.vendor }}' = 'embedded' ]]; then
        echo Configure local jdbc connection
        ambari-server setup --silent --java-home $JAVA_HOME
    else
        echo Configure remote jdbc connection
        local specoptions=''
        if [[ -f /var/lib/ambari-server/resources/Ambari-DDL-{{ ambari_database.fancyName }}-CREATE.sql ]]; then
          echo 'Initialize {{ ambari_database.vendor }} database for Ambari.'
          if [[ '{{ ambari_database.vendor }}' = 'postgres' ]]; then
            PGPASSWORD='{{ ambari_database.password }}' psql 'dbname={{ ambari_database.name }} options=--search_path=public' -h {{ ambari_database.host }} -p {{ ambari_database.port }} -U '{{ ambari_database.userName }}' -a -f /var/lib/ambari-server/resources/Ambari-DDL-{{ ambari_database.fancyName }}-CREATE.sql
            specoptions='--postgresschema public'
          fi
          if [[ '{{ ambari_database.vendor }}' = 'mysql' ]]; then
            mysql -h{{ ambari_database.host }} -P{{ ambari_database.port }} -u'{{ ambari_database.userName }}' -p'{{ ambari_database.password }}' '{{ ambari_database.name }}' < /var/lib/ambari-server/resources/Ambari-DDL-{{ ambari_database.fancyName }}-CREATE.sql
          fi
        else
            echo File not found /var/lib/ambari-server/resources/Ambari-DDL-{{ ambari_database.fancyName }}-CREATE.sql
            exit 1
        fi
        ambari-server setup --silent --verbose --java-home $JAVA_HOME \
            --database {{ ambari_database.vendor }} --databasehost {{ ambari_database.host }} --databaseport {{ ambari_database.port }} --databasename '{{ ambari_database.name }}' \
            --databaseusername '{{ ambari_database.userName }}' --databasepassword '{{ ambari_database.password }}' $specoptions
    fi
}

config_jdbc_drivers() {
  if [ -d "/var/lib/ambari-server/jdbc-drivers" ]; then
    if [ -z "$(find_and_distribute_latest_jdbc_driver postgres)" ]; then
      echo "PostgreSQL JDBC driver not found."
    fi
    if [ -z "$(find_and_distribute_latest_jdbc_driver mysql)" ]; then
      echo "MySQL JDBC driver not found."
    fi
  else
    echo "JDBC driver directory not found."
  fi
}

find_and_distribute_latest_jdbc_driver() {
    latest=$(find /var/lib/ambari-server/jdbc-drivers -name "$1*.jar" | tail -n1)
    if [ -z "$latest" ]; then
        exit 1
    fi
    ln -s $latest /usr/share/java # this is for ambari-server setup
    ln -s $latest /usr/lib/jvm/java/jre/lib/ext # this is for ambari-server start -> database check
    ambari-server setup --jdbc-db=$1 --jdbc-driver=${latest}
    echo ${latest}
}

# https://issues.apache.org/jira/browse/AMBARI-14627
silent_security_setup() {
  ambari-server setup-security --security-option=encrypt-passwords --master-key=bigdata --master-key-persist=true
}

main() {
  # consul-register-service ambari-server $(ip addr show eth0 | grep "inet\b" | awk '{print $2}' | cut -d/ -f1)
  if [ ! -f "/var/ambari-init-executed" ]; then
    config_jdbc_drivers
    config_remote_jdbc
    silent_security_setup
  fi
  echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/ambari-init-executed
}

main "$@"