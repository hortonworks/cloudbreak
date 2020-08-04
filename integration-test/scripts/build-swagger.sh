#!/usr/bin/env bash

date
echo -e "\n\033[1;96m--- build cloudbreak\033[0m\n"

../gradlew -p ../ \
    core:test --tests=com.sequenceiq.*.swagger.SwaggerGenerator \
    autoscale:test --tests=com.sequenceiq.*.swagger.SwaggerGenerator \
    environment:test --tests=com.sequenceiq.*.swagger.SwaggerGenerator \
    freeipa:test --tests=com.sequenceiq.*.swagger.SwaggerGenerator \
    redbeams:test --tests=com.sequenceiq.*.swagger.SwaggerGenerator \
    autoscale:test --tests=com.sequenceiq.*.swagger.SwaggerGenerator \
    datalake:test --tests=com.sequenceiq.*.swagger.SwaggerGenerator