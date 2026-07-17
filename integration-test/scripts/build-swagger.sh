#!/usr/bin/env bash

date
echo -e "\n\033[1;96m--- building OpenAPI definitions for services\033[0m\n"

source ./scripts/service-registry.sh

gradle_args=(clean)
for service in "${SERVICE_NAMES[@]}"; do
  gradle_args+=("${SERVICE_MODULE[$service]}:test" "--tests=com.sequenceiq.*.openapi.OpenApiGenerator")
done

../gradlew --no-build-cache -p ../ "${gradle_args[@]}"
