#!/usr/bin/env bash

date
echo -e "\n\033[1;96m--- building OpenAPI definitions for services\033[0m\n"

../gradlew --no-build-cache -p ../ \
    clean \
    core:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
    autoscale:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
    environment:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
    freeipa:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
    redbeams:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
    autoscale:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
    datalake:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
    externalized-compute:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator \
    environment-remote:test --tests=com.sequenceiq.*.openapi.OpenApiGenerator
