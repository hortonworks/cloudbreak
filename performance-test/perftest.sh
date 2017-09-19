CB_PERFTEST_HOST=192.168.99.100
CB_NUMBER_OF_USERS=3
CB_RAMPUP_SECONDS=3

docker run -it --rm \
-v `pwd`/conf:/opt/gatling/conf \
-v `pwd`/user-files:/opt/gatling/user-files \
-v `pwd`/results:/opt/gatling/results \
-e CB_PERFTEST_HOST=$CB_PERFTEST_HOST \
-e CB_NUMBER_OF_USERS=$CB_NUMBER_OF_USERS \
-e CB_RAMPUP_SECONDS=$CB_RAMPUP_SECONDS \
denvazh/gatling
