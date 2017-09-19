#!/bin/bash

: ${CB_PERFTEST_HOST:=192.168.99.100}
: ${CB_NUMBER_OF_USERS:=1}
: ${CB_RAMPUP_SECONDS:=3}
: ${CB_DELAY_BEFORE_TERM:=60}

docker run -it --rm \
-v `pwd`/conf:/opt/gatling/conf \
-v `pwd`/user-files:/opt/gatling/user-files \
-v `pwd`/results:/opt/gatling/results \
-e CB_PERFTEST_HOST=$CB_PERFTEST_HOST \
-e CB_NUMBER_OF_USERS=$CB_NUMBER_OF_USERS \
-e CB_RAMPUP_SECONDS=$CB_RAMPUP_SECONDS \
-e CB_DELAY_BEFORE_TERM=$CB_DELAY_BEFORE_TERM \
-e CB_USERNAME=$CB_USERNAME \
-e CB_PASSWORD=$CB_PASSWORD \
-e CB_HOSTNAME_ALIASES=$CB_HOSTNAME_ALIASES \
denvazh/gatling
