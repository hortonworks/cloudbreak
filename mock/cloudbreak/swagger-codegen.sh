#!/usr/bin/env bash

: ${CLOUDBREAK_ADDRESS?"Need to set CLOUDBREAK_ADDRESS"}

set -x

echo "GET https://${CLOUDBREAK_ADDRESS}/cb/api/swagger.json"
curl -o api/swagger.json -k https://${CLOUDBREAK_ADDRESS}/cb/api/swagger.json
echo "Generate model based on swagger JSON"
docker run --rm -it -v "${PWD}":"${PWD}" -w "${PWD}" --net=host swaggerapi/swagger-codegen-cli:v2.3.1 \
     generate \
       -i api/swagger.json \
       -l nodejs-server
#echo "Modify domain under src/app/models/cloudbreak/generated/api/"
#docker run --rm -it -v "${PWD}":"${PWD}" -w "${PWD}" alpine:3.5 sh  -c 'sed -i "s/\(protected basePath = \).*/\1'\''cloudbreak'\'';/" ./cloudbreak-ui/src/app/models/cloudbreak/generated/api/*'
#echo "kill all version"
#docker run --rm -it -v "${PWD}":"${PWD}" -w "${PWD}" alpine:3.5 sh \
#    -c 'sed -i "/OpenAPI/d" ./cloudbreak-ui/src/app/models/cloudbreak/generated/api/*'
#docker run --rm -it -v "${PWD}":"${PWD}" -w "${PWD}" alpine:3.5 sh \
#    -c 'sed -i "/OpenAPI/d" ./cloudbreak-ui/src/app/models/cloudbreak/generated/model/*'
#echo "Version was deleted from generated files."
#docker run -i stedolan/jq <cb-swagger.json -r '.info.version' > ./cloudbreak-ui/src/app/models/cloudbreak/VERSION
#echo "Remove unused files."
#rm -rf ./api-generator/cb-swagger.json || true
#rm -rf ./cloudbreak-ui/src/app/models/cloudbreak/generated/.swagger-codegen || true
#echo "Cloudbreak API updated."