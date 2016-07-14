#!/bin/bash

set -x

: ${JAVA_HOME:="/usr/lib/jvm/java"}

config_remote_jdbc() {
    if [[ '{{ ambari_database.vendor }}' = "embedded" ]]; then
        echo Configure local jdbc connection
        ambari-server setup --silent --java-home $JAVA_HOME
    else
        echo Configure remote jdbc connection
        local postgresschema=""
        if [[ -f /var/lib/ambari-server/resources/Ambari-DDL-{{ ambari_database.fancyName }}-CREATE.sql ]]; then
          if [[ '{{ ambari_database.vendor }}' = "postgres" ]]; then
            echo "Initialize database for Ambari."
            PGPASSWORD='{{ ambari_database.password }}' psql "dbname={{ ambari_database.name }} options=--search_path=public" -h {{ ambari_database.host }} -U {{ ambari_database.userName }} -a -f /var/lib/ambari-server/resources/Ambari-DDL-{{ ambari_database.fancyName }}-CREATE.sql
            postgresschema="--postgresschema public"
          fi
        else
            echo File not found /var/lib/ambari-server/resources/Ambari-DDL-{{ ambari_database.fancyName }}-CREATE.sql
            exit 1
        fi
        echo '{{ ambari_database.vendor }}://{{ ambari_database.userName }}:{{ ambari_database.password }}@{{ ambari_database.host }}:{{ ambari_database.port }}/{{ ambari_database.name }}'
        ambari-server setup --silent --verbose --java-home $JAVA_HOME \
            --database {{ ambari_database.vendor }} --databasehost {{ ambari_database.host }} --databaseport {{ ambari_database.port }} --databasename {{ ambari_database.name }} \
            --databaseusername {{ ambari_database.userName }} --databasepassword {{ ambari_database.password }} $postgresschema

    fi
}

config_jdbc_drivers() {
  JDBC_DRIVER_DIR="/var/lib/ambari-server/jdbc-drivers"
  if [ -d "$JDBC_DRIVER_DIR" ]; then
    LATEST_PSQL_DRIVER=$(find /var/lib/ambari-server/jdbc-drivers -name "postgresql*.jar" | tail -n1)
    if [ -n "$LATEST_PSQL_DRIVER" ]; then
      echo "Configure Postgres driver."
      ambari-server setup --jdbc-db=postgres --jdbc-driver=${LATEST_PSQL_DRIVER}
    else
      echo "PostgreSQL JDBC driver not found."
    fi
    LATEST_MYSQL_DRIVER=$(find /var/lib/ambari-server/jdbc-drivers -name "mysql*.jar" | tail -n1)
    if [ -n "$LATEST_MYSQL_DRIVER" ]; then
      echo "Configure Mysql driver."
      ambari-server setup --jdbc-db=mysql --jdbc-driver=${LATEST_MYSQL_DRIVER}
    else
      echo "MySQL JDBC driver not found."
    fi
  else
    echo "JDBC driver directory not found."
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
    config_jdbc_drivers
  fi
  echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/ambari-init-executed
}

main "$@"