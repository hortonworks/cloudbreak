#!/bin/bash

USAGE="Usage: ./run_cloudbreak.sh \
<host-addr> \
<db-user> \
<db-pass> \
<db-host> \
<db-port> \
<hbm2ddl-strategy> \
<smtp-username> \
<smpt-password \
<smtp-host> \
<smtp-port \
<mail-sender-from>"

# the address where the app is hosted
: ${HOST_ADDR:="$1"}

# database settings
: ${DB_ENV_USER:="$2"}
: ${DB_ENV_PASS:="$3"}
: ${DB_PORT_5432_TCP_ADDR:="$4"}
: ${DB_PORT_5432_TCP_PORT:="$5"}
: ${HBM2DDL_STRATEGY:="create"}

# SMTP properties
: ${MAIL_SENDER_USERNAME:="$6"}
: ${MAIL_SENDER_PASSWORD:="$7"}
: ${MAIL_SENDER_HOST:="$8"}
: ${MAIL_SENDER_PORT:="$9"}
: ${MAIL_SENDER_FROM:="$10"}

PROPERTIES=(\
  "HOST_ADDR" \
  "DB_ENV_USER" \
  "DB_ENV_PASS" \
  "DB_PORT_5432_TCP_ADDR" \
  "DB_PORT_5432_TCP_PORT" \
  "HBM2DDL_STRATEGY" \
  "MAIL_SENDER_USERNAME" \
  "MAIL_SENDER_PASSWORD" \
  "MAIL_SENDER_HOST" \
  "MAIL_SENDER_PORT" \
  "MAIL_SENDER_FROM"\
)

checkProps(){
for arg in "${PROPERTIES[@]}";
  do
    if [[ -z ${!arg} ]];
      then
        echo "Please set the property $arg . export $arg="
        break
      else
        echo "$arg"="${!arg}"
    fi
  done;
}

args(){
  for arg in "${PROPERTIES[@]}";
    do
      echo -n "-D$(lc $arg)=${!arg} ";
    done;
}

lc(){
    echo $1 | sed -e 's/\(.*\)/\L\1/' | sed 's/_/\./g'
}

checkProps

java $(args) -jar build/libs/cloudbreak-0.1-DEV.jar
