#!/bin/bash

set -ex

JAVA_PROFILE=/etc/profile.d/java.sh
if [ -f $JAVA_PROFILE ]
then
    echo "Setting JAVA_HOME from $JAVA_PROFILE"
    source $JAVA_PROFILE
fi

: ${JAVA_HOME:="/usr/lib/jvm/java"}
echo "JAVA_HOME set to $JAVA_HOME"

GPL_SWITCH=""

if [[ '{{ is_gpl_repo_enabled }}' = 'True' ]]; then
   echo "GPL accepted, enable LZO"
   GPL_SWITCH="--enable-lzo-under-gpl-license"
fi

config_remote_jdbc() {
    if [[ '{{ ambari_database.ambariVendor }}' = 'embedded' ]]; then
        echo Configure local jdbc connection
        ambari-server setup --silent $GPL_SWITCH --java-home $JAVA_HOME
    else
        echo Configure remote jdbc connection
        local specoptions=''
        echo 'Initialize {{ ambari_database.ambariVendor }} database for Ambari.'
        if [[ '{{ ambari_database.ambariVendor }}' = 'postgres' && -f /var/lib/ambari-server/resources/Ambari-DDL-Postgres-CREATE.sql ]]; then
          PGPASSWORD='{{ ambari_database.connectionPassword }}' psql 'dbname={{ ambari_database.databaseName }} options=--search_path=public' -h {{ ambari_database.host }} -p {{ ambari_database.port }} -U '{{ ambari_database.connectionUserName }}' -a -f /var/lib/ambari-server/resources/Ambari-DDL-Postgres-CREATE.sql
          specoptions='--postgresschema public'
        elif [[ '{{ ambari_database.ambariVendor }}' = 'mysql' && -f /var/lib/ambari-server/resources/Ambari-DDL-MySQL-CREATE.sql ]]; then
          mysql -h{{ ambari_database.host }} -P{{ ambari_database.port }} -u'{{ ambari_database.connectionUserName }}' -p'{{ ambari_database.connectionPassword }}' '{{ ambari_database.databaseName }}' < /var/lib/ambari-server/resources/Ambari-DDL-MySQL-CREATE.sql
          cp -f $(find /usr/share/java -name "mysql-connector*.jar" | tail -n1) /usr/share/java/mysql-connector.jar ##copy the jdbc driver jar to /usr/share/java folder because Ambari server requires it in that directory
          ambari-server setup --jdbc-db mysql --jdbc-driver /usr/share/java/mysql-connector.jar
          echo "server.jdbc.driver.path=/usr/share/java/mysql-connector.jar" >> /etc/ambari-server/conf/ambari.properties
        elif [[ '{{ ambari_database.ambariVendor }}' = 'oracle' && -f /var/lib/ambari-server/resources/Ambari-DDL-Oracle-CREATE.sql ]]; then
          sqlplus '{{ ambari_database.connectionUserName }}/{{ ambari_database.connectionPassword }}@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(Host={{ ambari_database.host }})(Port={{ ambari_database.port }}))(CONNECT_DATA=(SID=remote_SID)))' < /var/lib/ambari-server/resources/Ambari-DDL-Oracle-CREATE.sql
        else
            echo "SQL migration file could not be found for vendor='{{ ambari_database.ambariVendor }}'"
            exit 1
        fi
        ambari-server setup --silent $GPL_SWITCH --verbose --java-home $JAVA_HOME \
            --database {{ ambari_database.ambariVendor }} --databasehost {{ ambari_database.host }} --databaseport {{ ambari_database.port }} --databasename '{{ ambari_database.databaseName }}' \
            --databaseusername '{{ ambari_database.connectionUserName }}' --databasepassword '{{ ambari_database.connectionPassword }}' $specoptions
    fi
}

config_jdbc_drivers() {
    if [ -z "$(find_and_distribute_latest_jdbc_driver postgresql-jdbc postgres)" ]; then
      echo "PostgreSQL JDBC driver not found."
    fi
    if [ -z "$(find_and_distribute_latest_jdbc_driver mysql-connector mysql)" ]; then
      echo "MySQL JDBC driver not found."
    fi
    if [ -z "$(find_and_distribute_latest_jdbc_driver ojdbc oracle)" ]; then
      echo "ORACLE_JDBC driver not found."
    fi
}

find_and_distribute_latest_jdbc_driver() {
    latest=$(find /usr/share/java -name "$1*.jar" | tail -n1)
    if [ -z "$latest" ]; then
        exit 1
    fi

    ln -s $latest $JAVA_HOME/jre/lib/ext # this is for ambari-server start -> database check
    ambari-server setup --jdbc-db=$2 --jdbc-driver=${latest} $GPL_SWITCH
    echo ${latest}
}

# https://issues.apache.org/jira/browse/AMBARI-14627
silent_security_setup() {
  ambari-server setup-security --security-option=encrypt-passwords --master-key='{{ security_master_key }}' --master-key-persist=true
}

read_tarballs() {
  mkdir -p /opt/salt/preload
  cp -fn $(find /usr/hdp/ -name "mapreduce.tar.gz") /opt/salt/preload &
  cp -fn $(find /usr/hdp/ -name "tez.tar.gz") /opt/salt/preload &
  cp -fn $(find /usr/hdp/ -name "slider.tar.gz") /opt/salt/preload &
  cp -fn $(find /usr/hdp/ -name "pig.tar.gz") /opt/salt/preload &
}

main() {
  # consul-register-service ambari-server $(ip addr show eth0 | grep "inet\b" | awk '{print $2}' | cut -d/ -f1)
  echo "--------- Executing script: /opt/ambari-server/ambari-server-init.sh at $(date) ---------"
  config_jdbc_drivers
  config_remote_jdbc
  silent_security_setup
  # read_tarballs
  echo "--------- Executed script: /opt/ambari-server/ambari-server-init.sh at $(date) ---------"
  echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/ambari-init-executed
}

main "$@"