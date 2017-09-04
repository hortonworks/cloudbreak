CB_PERFTEST_HOST=192.168.99.100


docker run -it --rm \
-v `pwd`/conf:/opt/gatling/conf \
-v `pwd`/user-files:/opt/gatling/user-files \
-v `pwd`/results:/opt/gatling/results \
-e CB_PERFTEST_HOST=$CB_PERFTEST_HOST \
denvazh/gatling
