#!/bin/bash

USAGE="Usage: ./run_cloudbreak.sh <db-user> <db-pass> <db-host> <db-port> <host-address>"

if [ $# -lt 5 ];
    then
      echo ${USAGE}
      exit 1
fi

# database settings
: ${DB_ENV_USER:="$1"}
: ${DB_ENV_PASS:="$2"}
: ${DB_PORT_5432_TCP_ADDR:="$3"}
: ${DB_PORT_5432_TCP_PORT:="$4"}
: ${HBM2DDL_STRATEGY:="create"}
: ${HOST_ADDR:="$5"}

args(){
  for arg in DB_ENV_USER DB_ENV_PASS DB_PORT_5432_TCP_ADDR DB_PORT_5432_TCP_PORT HBM2DDL_STRATEGY HOST_ADDR; do
    echo -n "-D$arg=${!arg} ";
  done;
}

java $(args) -jar build/libs/cloudbreak-0.1-DEV.jar
